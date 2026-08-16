import type { IssPosition } from "../types/prediction";
import { FreshnessBadge } from "./FreshnessBadge";

interface PositionCardProps {
  position: IssPosition;
}

export function PositionCard({ position }: PositionCardProps) {
  return (
    <div className="position-card">
      <div className="position-card__header">
        <h3>Current Position</h3>
        <FreshnessBadge freshness={position.freshness} />
      </div>
      <dl>
        <dt>Latitude</dt>
        <dd>{position.latitude.toFixed(2)}°</dd>
        <dt>Longitude</dt>
        <dd>{position.longitude.toFixed(2)}°</dd>
        <dt>Altitude</dt>
        <dd>{position.altitudeKm !== null ? `${position.altitudeKm.toFixed(1)} km` : "Unavailable"}</dd>
        <dt>Velocity</dt>
        <dd>{position.velocityKmh !== null ? `${position.velocityKmh.toFixed(0)} km/h` : "Unavailable"}</dd>
      </dl>
      <p className="position-card__timestamp">
        As of {new Date(position.observedAt).toLocaleTimeString()}
      </p>
    </div>
  );
}