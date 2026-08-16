import type { IssPosition } from "../types/prediction";
import { EarthPreview } from "./EarthPreview";
import { FreshnessBadge } from "./FreshnessBadge";

interface HeroPanelProps {
  position: IssPosition;
}

export function HeroPanel({ position }: HeroPanelProps) {
  return (
    <div className="hero">
      <EarthPreview />
      <div>
        <div className="hero__stats-header">
          <span className="hero__stats-label">Current position</span>
          <FreshnessBadge freshness={position.freshness} />
        </div>
        <dl className="hero__grid">
          <dt>Lat</dt><dd>{position.latitude.toFixed(2)}°</dd>
          <dt>Lon</dt><dd>{position.longitude.toFixed(2)}°</dd>
          <dt>Alt</dt><dd>{position.altitudeKm !== null ? `${position.altitudeKm.toFixed(1)} km` : "Unavailable"}</dd>
          <dt>Vel</dt><dd>{position.velocityKmh !== null ? `${position.velocityKmh.toLocaleString()} km/h` : "Unavailable"}</dd>
        </dl>
        <p className="hero__timestamp">As of {new Date(position.observedAt).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" })}</p>
      </div>
    </div>
  );
}