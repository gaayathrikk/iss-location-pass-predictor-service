package com.isspredictor.iss_predictor_service.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Maps to the expected response from {@code https://iss-api.polluxlabs.io/iss-pass}.
 * <p>
 * <b>Assumption flagged:</b> polluxlabs.io's public site is a DIY-electronics blog with
 * no published API schema for this endpoint at the time this was written. The shape
 * below follows the de-facto standard used by comparable pass-prediction services
 * (the retired {@code open-notify iss-pass.json} format and N2YO-style APIs):
 * a list of passes, each with rise/set/culmination epoch times, duration, max
 * elevation, and rise/set azimuth in degrees.
 * <p>
 * Because this assumption could be wrong, {@link com.isspredictor.client.PolluxLabsClient}
 * treats any deserialization failure or unexpected shape as an upstream failure and
 * falls through to the Option A stale-cache/mock path — the app never crashes on a
 * schema mismatch, it degrades.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PolluxPassResponse(
        List<PassRaw> passes
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PassRaw(
            @JsonProperty("rise_time") long riseTimeEpochSeconds,
            @JsonProperty("set_time") long setTimeEpochSeconds,
            @JsonProperty("culmination_time") long culminationTimeEpochSeconds,
            @JsonProperty("duration_seconds") int durationSeconds,
            @JsonProperty("max_elevation_deg") double maxElevationDegrees,
            @JsonProperty("rise_azimuth_deg") double riseAzimuthDegrees,
            @JsonProperty("set_azimuth_deg") double setAzimuthDegrees
    ) {
    }
}