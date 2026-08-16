import type {
  Coordinates,
  VisibilityAssessment,
  IssPosition,
  PassPrediction,
} from "../types/prediction";

function mockPosition(coordinates: Coordinates): IssPosition {
  return {
    latitude: coordinates.latitude + 12.4,
    longitude: coordinates.longitude - 38.1,
    altitudeKm: 421.3,
    velocityKmh: 27600,
    observedAt: new Date().toISOString(),
    freshness: "MOCK",
  };
}

function mockPass(hoursFromNow: number): PassPrediction {
  const rise = new Date(Date.now() + hoursFromNow * 3_600_000);
  const culmination = new Date(rise.getTime() + 3 * 60_000);
  const set = new Date(rise.getTime() + 6 * 60_000);

  return {
    riseTime: rise.toISOString(),
    culminationTime: culmination.toISOString(),
    setTime: set.toISOString(),
    riseAzimuthCompass: "SW",
    setAzimuthCompass: "NE",
    riseAzimuthDegrees: 225,
    setAzimuthDegrees: 45,
    maxElevationDegrees: 62,
    magnitude: -3.2,
    duration: "PT6M",
    freshness: "MOCK",
  };
}

export function getMockAssessment(coordinates: Coordinates): VisibilityAssessment {
  return {
    location: coordinates,
    currentPosition: mockPosition(coordinates),
    upcomingPasses: [mockPass(2.5), mockPass(14.2), mockPass(26.8)],
    alerts: ["This is mock data — offline mode is active."],
    overallFreshness: "MOCK",
    _links: {
      self: { href: "#mock" },
      refresh: { href: "#mock" },
    },
  };
}