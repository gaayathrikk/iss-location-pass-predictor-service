import type { IssPosition } from "../types/prediction";
import { FreshnessBadge } from "./FreshnessBadge";

interface PositionPanelProps {
  position: IssPosition | null;
}

export function PositionPanel({ position }: PositionPanelProps) {
  return (
    <div className="layout-widget">
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "baseline" }}>
        <span className="layout-widget__label">Current position</span>
        {position && <FreshnessBadge freshness={position.freshness} />}
      </div>
      {position ? (
        <>
          <dl className="hero__grid">
            <dt>Lat</dt><dd>{position.latitude.toFixed(2)}°</dd>
            <dt>Lon</dt><dd>{position.longitude.toFixed(2)}°</dd>
            <dt>Alt</dt><dd>{position.altitudeKm !== null ? `${position.altitudeKm.toFixed(1)} km` : "—"}</dd>
            <dt>Vel</dt><dd>{position.velocityKmh !== null ? `${position.velocityKmh.toLocaleString()} km/h` : "—"}</dd>
          </dl>
          <p className="hero__timestamp">
            As of {new Date(position.observedAt).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" })}
          </p>
        </>
      ) : (
        <p className="layout-empty-hint">No data yet — find passes to see the ISS's current position.</p>
      )}
    </div>
  );
}