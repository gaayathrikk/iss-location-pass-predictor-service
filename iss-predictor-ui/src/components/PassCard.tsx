// import type { PassPrediction } from "../types/prediction";
// import { FreshnessBadge } from "./FreshnessBadge";

// interface PassCardProps {
//   pass: PassPrediction;
// }

// function formatTime(iso: string): string {
//   return new Date(iso).toLocaleString([], {
//     weekday: "short",
//     hour: "2-digit",
//     minute: "2-digit",
//   });
// }

// export function PassCard({ pass }: PassCardProps) {
//   return (
//     <div className="pass-card">
//       <div className="pass-card__header">
//         <h4>{formatTime(pass.riseTime)}</h4>
//         <FreshnessBadge freshness={pass.freshness} />
//       </div>
//       <dl>
//         <dt>Rises</dt>
//         <dd>{pass.riseAzimuthCompass} ({pass.riseAzimuthDegrees}°)</dd>
//         <dt>Max elevation</dt>
//         <dd>{pass.maxElevationDegrees}°</dd>
//         <dt>Sets</dt>
//         <dd>{pass.setAzimuthCompass} ({pass.setAzimuthDegrees}°)</dd>
//         <dt>Brightness</dt>
//         <dd>{pass.magnitude !== null ? `Mag ${pass.magnitude.toFixed(1)}` : "Unknown"}</dd>
//         <dt>Duration</dt>
//         <dd>{pass.duration.replace("PT", "").toLowerCase()}</dd>
//       </dl>
//     </div>
//   );
// }

import type { PassPrediction } from "../types/prediction";
import { FreshnessBadge } from "./FreshnessBadge";

interface PassCardProps {
  pass: PassPrediction;
}

const BASELINE_Y = 76;
const MAX_ARC_HEIGHT = 60;
const RISE_X = 20;
const SET_X = 200;
const MID_X = 110;

function arcControlY(elevationDegrees: number): number {
  const clamped = Math.max(0, Math.min(90, elevationDegrees));
  const peakY = BASELINE_Y - (clamped / 90) * MAX_ARC_HEIGHT;
  return 2 * peakY - BASELINE_Y;
}

function formatTime(iso: string): string {
  return new Date(iso).toLocaleString([], { weekday: "short", hour: "numeric", minute: "2-digit" });
}

export function PassCard({ pass }: PassCardProps) {
  const controlY = arcControlY(pass.maxElevationDegrees);
  const peakY = BASELINE_Y - (Math.min(pass.maxElevationDegrees, 90) / 90) * MAX_ARC_HEIGHT;

  return (
    <div className="pass-card">
      <div className="pass-card__header">
        <span className="pass-card__time">{formatTime(pass.riseTime)}</span>
        <FreshnessBadge freshness={pass.freshness} />
      </div>
      <svg width="100%" viewBox="0 0 220 90" role="img" aria-label={`Sky path rising ${pass.riseAzimuthCompass}, peaking at ${pass.maxElevationDegrees} degrees, setting ${pass.setAzimuthCompass}`}>
        <line x1="8" y1={BASELINE_Y} x2="212" y2={BASELINE_Y} stroke="#4a3d6b" strokeWidth="1" />
        <path
          d={`M ${RISE_X} ${BASELINE_Y} Q ${MID_X} ${controlY} ${SET_X} ${BASELINE_Y}`}
          fill="none"
          stroke="#d8ecff"
          strokeWidth="1.5"
          strokeDasharray="1 6"
        />
        <circle cx={RISE_X} cy={BASELINE_Y} r="3" fill="#ff9466" />
        <circle cx={SET_X} cy={BASELINE_Y} r="3" fill="#ff9466" />
        <circle cx={MID_X} cy={peakY} r="4" fill="#d8ecff" />
        <text x={RISE_X} y={BASELINE_Y + 14} fontSize="9" fill="#8779a8" textAnchor="middle">{pass.riseAzimuthCompass}</text>
        <text x={SET_X} y={BASELINE_Y + 14} fontSize="9" fill="#8779a8" textAnchor="middle">{pass.setAzimuthCompass}</text>
        <text x={MID_X} y={peakY - 8} fontSize="10" fill="#f3f1fb" textAnchor="middle">{pass.maxElevationDegrees}°</text>
      </svg>
      <div className="pass-card__meta">
        {pass.magnitude !== null ? `Mag ${pass.magnitude.toFixed(1)}` : "Mag unknown"} · {pass.duration.replace("PT", "").toLowerCase()}
      </div>
    </div>
  );
}