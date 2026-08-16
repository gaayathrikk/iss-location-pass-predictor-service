package com.isspredictor.iss_predictor_service.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Maps 1:1 to the raw response from {@code http://api.open-notify.org/iss-now.json}:
 * <pre>
 * {
 *   "message": "success",
 *   "timestamp": 1690000000,
 *   "iss_position": { "latitude": "12.34", "longitude": "56.78" }
 * }
 * </pre>
 * Note Open-Notify returns lat/lon as strings, not numbers — that quirk is
 * absorbed here so nothing downstream has to know about it.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OpenNotifyResponse(
        String message,
        long timestamp,
        @JsonProperty("iss_position") IssPositionRaw issPosition
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record IssPositionRaw(String latitude, String longitude) {
    }
}