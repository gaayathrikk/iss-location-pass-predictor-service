package com.isspredictor.iss_predictor_service.model;

import lombok.Builder;
import lombok.Value;

import java.time.Duration;
import java.time.Instant;

/**
 * A single predicted ISS pass over a given location: when it rises, culminates,
 * and sets, plus the derived viewing metadata (direction, brightness) that the
 * alert engine (Phase 3) reads to decide which conditional messages to show.
 */
@Value
@Builder(toBuilder = true)
public class PassPrediction {

    Instant riseTime;
    Instant culminationTime;
    Instant setTime;

    /** Compass direction (e.g. "NW") the ISS rises in. */
    String riseAzimuthCompass;

    /** Compass direction (e.g. "SE") the ISS sets in. */
    String setAzimuthCompass;

    /** Raw azimuth in degrees at rise, kept alongside the compass label for API consumers that want the number. */
    double riseAzimuthDegrees;
    double setAzimuthDegrees;

    /** Highest elevation angle (degrees above horizon) reached during the pass. */
    double maxElevationDegrees;

    /** Estimated visual magnitude at max elevation; lower (more negative) = brighter. */
    Double magnitude;

    Duration duration;

    DataFreshness freshness;

    public boolean exceedsDuration(Duration threshold) {
        return duration != null && duration.compareTo(threshold) > 0;
    }

    public boolean isBrighterThan(double magnitudeThreshold) {
        return magnitude != null && magnitude < magnitudeThreshold;
    }
}