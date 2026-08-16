import type { DataFreshness } from "../types/prediction";

interface FreshnessBadgeProps {
  freshness: DataFreshness;
}

const LABELS: Record<DataFreshness, string> = {
  LIVE: "Live",
  CACHED_STALE: "Cached",
  MOCK: "Mock",
};


export function FreshnessBadge({ freshness }: FreshnessBadgeProps) {
  return (
    <span className={`freshness-badge freshness-badge--${freshness.toLowerCase()}`}>
      {LABELS[freshness]}
    </span>
  );
}