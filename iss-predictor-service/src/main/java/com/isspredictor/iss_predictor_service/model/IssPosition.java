package com.isspredictor.iss_predictor_service.model;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

/**
 * Current ISS position, altitude and velocity.
 * <p>
 * Open-Notify's raw response only gives latitude/longitude — altitude and
 * velocity are not part of that feed. Rather than baking that limitation
 * into every caller, this model always exposes all four fields; the service
 * layer decides (Phase 3) whether to compute/estimate altitude & velocity
 * or mark them absent. Keeping the model's shape independent of any single
 * upstream's limitations is the point of the anti-corruption layer.
 */
@Value
@Builder(toBuilder = true)
public class IssPosition {

    double latitude;
    double longitude;

    /** Kilometres above sea level. Nullable: not all sources supply this. */
    Double altitudeKm;

    /** Kilometres per hour. Nullable: not all sources supply this. */
    Double velocityKmh;

    Instant observedAt;

    DataFreshness freshness;
}