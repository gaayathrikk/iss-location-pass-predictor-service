package com.isspredictor.iss_predictor_service.model;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.util.List;

/**
 * The single aggregated object returned by the API: current ISS position,
 * the list of upcoming passes for the requested location (each already
 * paired with its relevant cloud forecast), and the conditional alert
 * messages produced by the visibility engine (Phase 3).
 * <p>
 * This is intentionally a flat, self-contained response — the frontend and
 * Postman users should never need to make a second call to assemble a
 * complete picture for one location.
 */
@Value
@Builder
public class VisibilityAssessment {

    Coordinates location;

    IssPosition currentPosition;

    @Singular
    List<PassPrediction> upcomingPasses;

    @Singular
    List<String> alerts;

    /** Worst (least fresh) freshness across all constituent data sources, so the client has one field to check. */
    DataFreshness overallFreshness;
}