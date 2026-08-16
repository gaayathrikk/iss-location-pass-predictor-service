export type DataFreshness = "LIVE" | "CACHED_STALE" | "MOCK";

export interface Coordinates {
  latitude: number;
  longitude: number;
}

export interface IssPosition {
  latitude: number;
  longitude: number;
  altitudeKm: number | null;
  velocityKmh: number | null;
  observedAt: string; // ISO instant
  freshness: DataFreshness;
}

export interface PassPrediction {
  riseTime: string; // ISO instant
  culminationTime: string;
  setTime: string;
  riseAzimuthCompass: string;
  setAzimuthCompass: string;
  riseAzimuthDegrees: number;
  setAzimuthDegrees: number;
  maxElevationDegrees: number;
  magnitude: number | null;
  duration: string; // ISO-8601 duration, e.g. "PT7M"
  freshness: DataFreshness;
}

export interface HateoasLink {
  href: string;
}

export interface VisibilityAssessment {
  location: Coordinates;
  currentPosition: IssPosition;
  upcomingPasses: PassPrediction[];
  alerts: string[];
  overallFreshness: DataFreshness;
  _links: {
    self: HateoasLink;
    refresh: HateoasLink;
  };
}