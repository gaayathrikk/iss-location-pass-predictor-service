import type { PassPrediction } from "../types/prediction";

interface PassesPanelProps {
  passes: PassPrediction[] | null;
}

function formatTime(iso: string): string {
  return new Date(iso).toLocaleString([], { weekday: "short", hour: "numeric", minute: "2-digit" });
}

export function PassesPanel({ passes }: PassesPanelProps) {
  return (
    <div className="layout-widget">
      <div className="layout-widget__label">Upcoming passes</div>
      {passes === null ? (
        <p className="layout-empty-hint">No data yet — find passes to see upcoming visibility windows.</p>
      ) : passes.length === 0 ? (
        <p className="layout-empty-hint">No visible passes in the current forecast window.</p>
      ) : (
        <ul className="hud-pass-list">
          {passes.map((pass) => (
            <li key={pass.riseTime} className="hud-pass-list__item">
              <div className="hud-pass-list__time">{formatTime(pass.riseTime)}</div>
              <div className="hud-pass-list__meta">
                {pass.riseAzimuthCompass} → {pass.maxElevationDegrees}° → {pass.setAzimuthCompass}
                {" · "}
                {pass.magnitude !== null ? `Mag ${pass.magnitude.toFixed(1)}` : "Mag —"}
              </div>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}