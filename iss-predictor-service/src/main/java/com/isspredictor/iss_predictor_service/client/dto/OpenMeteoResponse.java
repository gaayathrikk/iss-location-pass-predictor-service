/**
 * Wire-format DTOs matching each upstream API raw JSON shape. Kept separate from internal domain model so upstream schema drift does not leak inward (anti-corruption layer).
 */
package com.isspredictor.iss_predictor_service.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Maps 1:1 to Open-Meteo's response for
 * {@code /v1/forecast?...&hourly=cloud_cover}:
 * <pre>
 * {
 *   "hourly": {
 *     "time": ["2026-08-03T00:00", "2026-08-03T01:00", ...],
 *     "cloud_cover": [40, 55, ...]
 *   }
 * }
 * </pre>
 * Open-Meteo returns two parallel arrays rather than a list of objects —
 * that pairing-by-index quirk is absorbed here (see
 * {@code OpenMeteoClient#toCloudForecasts}) so the domain model never has
 * to deal with parallel arrays.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OpenMeteoResponse(
        Hourly hourly
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Hourly(
            List<String> time,
            @JsonProperty("cloud_cover") List<Integer> cloudCover
    ) {
    }
}