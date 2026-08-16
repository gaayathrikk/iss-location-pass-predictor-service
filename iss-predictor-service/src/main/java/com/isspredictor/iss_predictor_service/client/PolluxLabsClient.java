package com.isspredictor.iss_predictor_service.client;

import com.isspredictor.iss_predictor_service.cache.TtlCache;
import com.isspredictor.iss_predictor_service.client.dto.PolluxPassResponse;
import com.isspredictor.iss_predictor_service.model.Coordinates;
import com.isspredictor.iss_predictor_service.model.DataFreshness;
import com.isspredictor.iss_predictor_service.model.PassPrediction;
import com.isspredictor.iss_predictor_service.util.AzimuthFormatter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

/**
 * Implements {@link PassPredictionClient} against Pollux Labs' iss-pass endpoint.
 * <p>
 * Note: {@code magnitude} is deliberately left {@code null} here - brightness
 * calculation from elevation angle belongs to the service-layer
 * {@code MagnitudeCalculator} (Phase 3), not this client. Keeping that math out
 * of the client keeps this class focused purely on "get data, map it, degrade
 * gracefully" - a single responsibility.
 */
@Component
@Slf4j
public class PolluxLabsClient implements PassPredictionClient {

    private final RestClient restClient;
    private final TtlCache<String, List<PassPrediction>> cache;
    private final Duration ttl;
    private final Duration maxStale;

    public PolluxLabsClient(
            @Qualifier("polluxLabsRestClient") RestClient restClient,
            @Qualifier("passCache") TtlCache<String, List<PassPrediction>> cache,
            @Value("${iss.cache.pass-prediction-ttl-seconds}") long ttlSeconds,
            @Value("${iss.cache.max-stale-serve-seconds}") long maxStaleSeconds) {
        this.restClient = restClient;
        this.cache = cache;
        this.ttl = Duration.ofSeconds(ttlSeconds);
        this.maxStale = Duration.ofSeconds(maxStaleSeconds);
    }

    @Override
    public List<PassPrediction> getUpcomingPasses(Coordinates location) {
        String key = cacheKey(location);
        return cache.getFresh(key)
                .orElseGet(() -> fetchLiveOrDegrade(location, key));
    }

    private List<PassPrediction> fetchLiveOrDegrade(Coordinates location, String key) {
        try {
            List<PassPrediction> live = fetchLive(location);
            cache.put(key, live, ttl);
            return live;
        } catch (RestClientException e) {
            log.warn("Pollux Labs call failed for {}, attempting degraded fallback: {}", key, e.getMessage());
            return cache.getStaleIfWithin(key, maxStale)
                    .map(this::downgradeToStale)
                    .orElseGet(this::mockPasses);
        }
    }

    private List<PassPrediction> fetchLive(Coordinates location) {
        PolluxPassResponse response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/iss-pass")
                        .queryParam("lat", location.latitude())
                        .queryParam("lon", location.longitude())
                        .build())
                .retrieve()
                .body(PolluxPassResponse.class);

        if (response == null || response.passes() == null) {
            throw new RestClientException("Pollux Labs returned an empty/unexpected body");
        }

        return response.passes().stream()
                .map(this::toPassPrediction)
                .toList();
    }

    private PassPrediction toPassPrediction(PolluxPassResponse.PassRaw raw) {
        return PassPrediction.builder()
                .riseTime(Instant.ofEpochSecond(raw.riseTimeEpochSeconds()))
                .culminationTime(Instant.ofEpochSecond(raw.culminationTimeEpochSeconds()))
                .setTime(Instant.ofEpochSecond(raw.setTimeEpochSeconds()))
                .riseAzimuthDegrees(raw.riseAzimuthDegrees())
                .setAzimuthDegrees(raw.setAzimuthDegrees())
                .riseAzimuthCompass(AzimuthFormatter.toCompass(raw.riseAzimuthDegrees()))
                .setAzimuthCompass(AzimuthFormatter.toCompass(raw.setAzimuthDegrees()))
                .maxElevationDegrees(raw.maxElevationDegrees())
                .duration(Duration.ofSeconds(raw.durationSeconds()))
                .magnitude(null) // filled in later by the service-layer MagnitudeCalculator
                .freshness(DataFreshness.LIVE)
                .build();
    }

    // /**
    //  * Basic 8-point compass conversion. Will move to a shared
    //  * {@code util.AzimuthFormatter} once the service layer exists (Phase 3) so
    //  * both this client and the alert engine use one implementation.
    //  */
    // private String toCompass(double degrees) {
    //     String[] points = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};
    //     int index = (int) Math.round(((degrees % 360) / 45.0)) % 8;
    //     return points[index];
    // }

    private List<PassPrediction> downgradeToStale(List<PassPrediction> passes) {
        return passes.stream()
                .map(p -> p.toBuilder().freshness(DataFreshness.CACHED_STALE).build())
                .toList();
    }

    private String cacheKey(Coordinates location) {
        return String.format(Locale.ROOT, "passes:%.2f,%.2f", location.latitude(), location.longitude());
    }

    /** Last-resort fallback: one plausible-looking pass, clearly tagged MOCK. */
    private List<PassPrediction> mockPasses() {
        log.warn("No stale cache available - serving mock pass prediction");
        Instant rise = Instant.now().plus(Duration.ofHours(1));
        return List.of(PassPrediction.builder()
                .riseTime(rise)
                .culminationTime(rise.plus(Duration.ofMinutes(3)))
                .setTime(rise.plus(Duration.ofMinutes(6)))
                .riseAzimuthDegrees(315.0)
                .setAzimuthDegrees(135.0)
                .riseAzimuthCompass("NW")
                .setAzimuthCompass("SE")
                .maxElevationDegrees(45.0)
                .duration(Duration.ofMinutes(6))
                .magnitude(null)
                .freshness(DataFreshness.MOCK)
                .build());
    }
}