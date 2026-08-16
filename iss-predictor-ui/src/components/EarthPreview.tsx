import { useEffect, useRef, useState } from "react";
import * as THREE from "three";
import type { Mesh, Vector3 } from "three";
import type { Coordinates } from "../types/prediction";
import { EarthPreviewFallback } from "./EarthPreviewFallback";

interface EarthPreviewProps {
  issPosition?: Coordinates;
  userLocation?: Coordinates | null;
}

const TEXTURES = {
  day: "/textures/earth/day.jpg",
  night: "/textures/earth/night.jpg",
  clouds: "/textures/earth/clouds.png",
};

const ISS_INCLINATION_DEG = 51.6;
const EARTH_RADIUS = 2.5;
const USER_MARKER_RADIUS = 2.52;
const ORBIT_RADIUS = 2.65; 

// Computes - Where is the Sun directly overhead right now?
function subsolarPoint(date: Date) {
  const start = Date.UTC(date.getUTCFullYear(), 0, 0);
  const dayOfYear = Math.floor((date.getTime() - start) / 86_400_000);
  const decl = -23.44 * Math.cos(((2 * Math.PI) / 365) * (dayOfYear + 10));
  const utcHours = date.getUTCHours() + date.getUTCMinutes() / 60;
  return { lon: -(utcHours - 12) * 15, lat: decl };
}

// ISS-Orbit Plane - determins plane's orientation and which direction the plane faces
function computeOrbitNormal(THREE: any, point: any, inclinationDeg: number) {
  const i = (inclinationDeg * Math.PI) / 180;
  const { x: Px, y: Py, z: Pz } = point;
  const R = Math.sqrt(Px * Px + Pz * Pz);
  const delta = Math.atan2(Pz, Px);
  const ratio = Math.max(-1, Math.min(1, -Py / Math.tan(i) / R));
  const node = delta + Math.acos(ratio);
  return new THREE.Vector3(Math.sin(i) * Math.cos(node), Math.cos(i), Math.sin(i) * Math.sin(node)).normalize();
}

// Calculates a point for every angle and then Three.js connects those points into a line.
function buildOrbitPoints(THREE: any, normal: any, radius: number, segments = 128) {
  const arbitrary = Math.abs(normal.y) < 0.99 ? new THREE.Vector3(0, 1, 0) : new THREE.Vector3(1, 0, 0);
  const u = new THREE.Vector3().crossVectors(arbitrary, normal).normalize();
  const v = new THREE.Vector3().crossVectors(normal, u).normalize();
  const points = [];
  for (let s = 0; s <= segments; s++) {
    const theta = (s / segments) * Math.PI * 2;
    points.push(new THREE.Vector3().addScaledVector(u, Math.cos(theta)).addScaledVector(v, Math.sin(theta)).multiplyScalar(radius));
  }
  return points;
}

function GlobeLegend() {
  return (
    <div className="layout-globe-legend">
      <span className="layout-globe-legend__item">
        <span className="layout-globe-legend__dot" style={{ background: "#d8ecff" }} />
        ISS
      </span>
      <span className="layout-globe-legend__item">
        <span className="layout-globe-legend__dot" style={{ background: "#ff9466" }} />
        Your location
      </span>
    </div>
  );
}

export function EarthPreview({ issPosition, userLocation }: EarthPreviewProps) {
  const containerRef = useRef<HTMLDivElement>(null);
  const [status, setStatus] = useState<"loading" | "ready" | "unsupported" | "error">("loading");

  const sceneHandleRef = useRef<{
    updateIss: (loc: Coordinates | undefined) => void;
    updateUser: (loc: Coordinates | null | undefined) => void;
  } | null>(null);

  useEffect(() => {
    const container = containerRef.current;
    if (!container) return;

    // Creates a fake canvas
    const probe = document.createElement("canvas");
    const hasWebGL = !!(probe.getContext("webgl") || probe.getContext("experimental-webgl"));
    if (!hasWebGL) {
      setStatus("unsupported");
      return;
    }

    let disposed = false;
    let animationFrameId = 0;
    let sunIntervalId = 0;
    let cleanupListeners = () => {};

    async function init() {
      const THREE = await import("three");
      if (disposed || !container) return;

      const size = container.clientWidth;
      // Engine - draws pixels
      const renderer = new THREE.WebGLRenderer({ antialias: true, alpha: true });
      renderer.setSize(size, size);
      renderer.setPixelRatio(window.devicePixelRatio || 1);
      container.replaceChildren(renderer.domElement);
      
      // The world
      const scene = new THREE.Scene();

      // The viewer
      const camera = new THREE.PerspectiveCamera(45, 1, 0.1, 100);
      camera.position.set(0, 0, 7);

      const starCount = 700;
      const starPositions = new Float32Array(starCount * 3);
      for (let i = 0; i < starCount; i++) {
        const r = 40 + Math.random() * 20;
        const theta = Math.random() * Math.PI * 2;
        const phi = Math.acos(2 * Math.random() - 1);
        starPositions[i * 3] = r * Math.sin(phi) * Math.cos(theta);
        starPositions[i * 3 + 1] = r * Math.sin(phi) * Math.sin(theta);
        starPositions[i * 3 + 2] = r * Math.cos(phi);
      }
      const starGeometry = new THREE.BufferGeometry();
      starGeometry.setAttribute("position", new THREE.BufferAttribute(starPositions, 3));
      scene.add(new THREE.Points(starGeometry, new THREE.PointsMaterial({ color: 0xffffff, size: 0.05 })));

      const globeGroup = new THREE.Group();
      scene.add(globeGroup);

      function latLonToXYZ(lon: number, lat: number, r: number): Vector3 {
        const phi = ((90 - lat) * Math.PI) / 180;
        const theta = ((lon + 180) * Math.PI) / 180;
        return new THREE.Vector3(
          -r * Math.sin(phi) * Math.cos(theta),
          r * Math.cos(phi),
          r * Math.sin(phi) * Math.sin(theta)
        );
      }

      let dayTex, nightTex, cloudsTex;
      try {
        const loader = new THREE.TextureLoader();
        [dayTex, nightTex, cloudsTex] = await Promise.all([
          loader.loadAsync(TEXTURES.day),
          loader.loadAsync(TEXTURES.night),
          loader.loadAsync(TEXTURES.clouds),
        ]);
      } catch (err) {
        console.error("EarthPreview: texture load failed", err);
        if (!disposed) setStatus("error");
        return;
      }
      if (disposed) return;

      const sun = subsolarPoint(new Date());
      const sunDirection = latLonToXYZ(sun.lon, sun.lat, 1).normalize();

      const earthMaterial = new THREE.ShaderMaterial({
        uniforms: {
          dayTexture: { value: dayTex },
          nightTexture: { value: nightTex },
          sunDirection: { value: sunDirection.clone() },
        },
        vertexShader: `
          varying vec3 vNormal;
          varying vec2 vUv;
          void main() {
            vNormal = normalize(normal);
            vUv = uv;
            gl_Position = projectionMatrix * modelViewMatrix * vec4(position, 1.0);
          }
        `,
        fragmentShader: `
          uniform sampler2D dayTexture;
          uniform sampler2D nightTexture;
          uniform vec3 sunDirection;
          varying vec3 vNormal;
          varying vec2 vUv;
          void main() {
            float intensity = dot(vNormal, normalize(sunDirection));
            float mixFactor = smoothstep(-0.15, 0.15, intensity);
            vec4 dayColor = texture2D(dayTexture, vUv);
            vec4 nightColor = texture2D(nightTexture, vUv) * 1.4;
            gl_FragColor = mix(nightColor, dayColor, mixFactor);
          }
        `,
      });

      const earthMesh = new THREE.Mesh(new THREE.SphereGeometry(EARTH_RADIUS, 64, 64), earthMaterial);
      globeGroup.add(earthMesh);

      const cloudsMesh = new THREE.Mesh(
        new THREE.SphereGeometry(EARTH_RADIUS+0.02, 64, 64),
        new THREE.MeshLambertMaterial({ map: cloudsTex, transparent: true, opacity: 0.35, depthWrite: false })
      );
      globeGroup.add(cloudsMesh);

      const atmosphereMesh = new THREE.Mesh(
        new THREE.SphereGeometry(EARTH_RADIUS+0.18, 64, 64),
        new THREE.ShaderMaterial({
          vertexShader: `
            varying vec3 vNormal;
            void main() {
              vNormal = normalize(normalMatrix * normal);
              gl_Position = projectionMatrix * modelViewMatrix * vec4(position, 1.0);
            }
          `,
          fragmentShader: `
            varying vec3 vNormal;
            void main() {
              float intensity = pow(0.65 - dot(vNormal, vec3(0.0, 0.0, 1.0)), 3.0);
              gl_FragColor = vec4(0.6, 0.75, 1.0, 1.0) * intensity;
            }
          `,
          blending: THREE.AdditiveBlending,
          side: THREE.BackSide,
          transparent: true,
        })
      );
      globeGroup.add(atmosphereMesh);

      const sunLight = new THREE.DirectionalLight(0xffffff, 1.0);
      sunLight.position.copy(sunDirection.clone().multiplyScalar(10));
      scene.add(sunLight);
      scene.add(new THREE.AmbientLight(0x222222, 0.6));

      let issMarker: Mesh | null = null;
      let issGlow: Mesh | null = null;
      let orbitLine: any = null;
      let userMarker: Mesh | null = null;

      function updateIss(loc: Coordinates | undefined) {
        if (!loc) return;
        const xyz = latLonToXYZ(loc.longitude, loc.latitude, ORBIT_RADIUS); // was 2.3

        if (!issMarker) {
          issMarker = new THREE.Mesh(new THREE.SphereGeometry(0.06, 12, 12), new THREE.MeshBasicMaterial({ color: 0xd8ecff }));
          issGlow = new THREE.Mesh(new THREE.SphereGeometry(0.1, 12, 12), new THREE.MeshBasicMaterial({ color: 0xd8ecff, transparent: true, opacity: 0.35 }));
          globeGroup.add(issMarker, issGlow);
        }
        issMarker.position.copy(xyz);
        issGlow!.position.copy(xyz);

        const pointOnSphere = latLonToXYZ(loc.longitude, loc.latitude, EARTH_RADIUS);
        const normal = computeOrbitNormal(THREE, pointOnSphere, ISS_INCLINATION_DEG);
        const orbitPoints = buildOrbitPoints(THREE, normal, ORBIT_RADIUS);

        if (orbitLine) {
          orbitLine.geometry.dispose();
          orbitLine.geometry = new THREE.BufferGeometry().setFromPoints(orbitPoints);
          orbitLine.computeLineDistances();
        } else {
          const geometry = new THREE.BufferGeometry().setFromPoints(orbitPoints);
          const material = new THREE.LineDashedMaterial({ color: 0xd8ecff, dashSize: 0.06, gapSize: 0.04, transparent: true, opacity: 0.55 });
          orbitLine = new THREE.Line(geometry, material);
          orbitLine.computeLineDistances();
          globeGroup.add(orbitLine);
        }
      }

      function updateUser(loc: Coordinates | null | undefined) {
        if (!loc) return;
        if (!userMarker) {
          userMarker = new THREE.Mesh(new THREE.SphereGeometry(0.05, 12, 12), new THREE.MeshBasicMaterial({ color: 0xff9466 }));
          globeGroup.add(userMarker);
        }
        userMarker.position.copy(latLonToXYZ(loc.longitude, loc.latitude, USER_MARKER_RADIUS));
      }

      updateIss(issPosition);
      updateUser(userLocation);
      sceneHandleRef.current = { updateIss, updateUser };

      sunIntervalId = window.setInterval(() => {
        const s = subsolarPoint(new Date());
        const dir = latLonToXYZ(s.lon, s.lat, 1).normalize();
        earthMaterial.uniforms.sunDirection.value = dir;
        sunLight.position.copy(dir.clone().multiplyScalar(10));
      }, 5 * 60 * 1000);

      let dragging = false, lastX = 0, lastY = 0, rotY = 0.4, rotX = 0.15, autoRotate = true;
      const dom = renderer.domElement;
      const onPointerDown = (e: PointerEvent) => { dragging = true; autoRotate = false; lastX = e.clientX; lastY = e.clientY; };
      const onPointerUp = () => { dragging = false; };
      const onPointerMove = (e: PointerEvent) => {
        if (!dragging) return;
        rotY += (e.clientX - lastX) * 0.005;
        rotX = Math.max(-1.2, Math.min(1.2, rotX + (e.clientY - lastY) * 0.005));
        lastX = e.clientX; lastY = e.clientY;
      };
      dom.addEventListener("pointerdown", onPointerDown);
      window.addEventListener("pointerup", onPointerUp);
      window.addEventListener("pointermove", onPointerMove);
      cleanupListeners = () => {
        dom.removeEventListener("pointerdown", onPointerDown);
        window.removeEventListener("pointerup", onPointerUp);
        window.removeEventListener("pointermove", onPointerMove);
      };

      function animate() {
        animationFrameId = requestAnimationFrame(animate);
        if (autoRotate) rotY += 0.0015;
        globeGroup.rotation.set(rotX, rotY, 0);
        cloudsMesh.rotation.y += 0.0004;
        renderer.render(scene, camera);
      }
      animate();
      setStatus("ready");

      cleanupListeners = ((prevCleanup) => () => {
        prevCleanup();
        cancelAnimationFrame(animationFrameId);
        clearInterval(sunIntervalId);
        scene.traverse((obj: THREE.Object3D) => {
          if (obj instanceof THREE.Mesh || obj instanceof THREE.Line) {
        // scene.traverse((obj) => {
        //   if (obj instanceof THREE.Mesh || obj instanceof THREE.Line) {
            obj.geometry?.dispose();
            const material = (obj as any).material;
            if (Array.isArray(material)) material.forEach((m) => m.dispose());
            else material?.dispose();
          }
        });
        dayTex?.dispose();
        nightTex?.dispose();
        cloudsTex?.dispose();
        renderer.dispose();
      })(cleanupListeners);
    }

    init();
    return () => {
      disposed = true;
      cleanupListeners();
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    sceneHandleRef.current?.updateIss(issPosition);
    sceneHandleRef.current?.updateUser(userLocation);
  }, [issPosition, userLocation]);

  if (status === "unsupported" || status === "error") {
  return (
    <div className="layout-globe-card">
      <EarthPreviewFallback />
      <GlobeLegend />
    </div>
  );
}

return (
  <div className="layout-globe-card">
    <div
      ref={containerRef}
      style={{ width: "100%", maxWidth: 420, aspectRatio: "1 / 1" }}
      role="img"
      aria-label="3D globe showing current ISS position with real day and night lighting"
    />
    {status === "loading" && <p style={{ fontSize: 11, color: "#6c5f8f", margin: "6px 0 0" }}>Loading Earth imagery…</p>}
    {status === "ready" && <GlobeLegend />}
  </div>
);
}