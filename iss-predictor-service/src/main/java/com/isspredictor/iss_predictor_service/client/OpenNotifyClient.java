package com.isspredictor.iss_predictor_service.client;

import com.isspredictor.iss_predictor_service.cache.TtlCache;
import com.isspredictor.iss_predictor_service.client.dto.OpenNotifyResponse;
import com.isspredictor.iss_predictor_service.model.DataFreshness;
import com.isspredictor.iss_predictor_service.model.IssPosition;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.time.Instant;

/**
 * Implements {@link IssPositionClient} against Open-Notify's iss-now.json feed.
 * <p>
 * Degradation order per Option A: fresh cache -> live call -> stale cache
 * (up to the configured ceiling) -> hardcoded mock. Every branch tags the
 * returned {@link IssPosition} with the correct {@link DataFreshness} so the
 * caller always knows exactly what it's getting.
 */
@Component
@Slf4j
public class OpenNotifyClient implements IssPositionClient {

    /** Position is location-independent, so a single constant cache key covers every request. */
    private static final String CACHE_KEY = "current";

    private final RestClient restClient;
    private final TtlCache<String, IssPosition> cache;
    private final Duration ttl;
    private final Duration maxStale;

    public OpenNotifyClient(
            @Qualifier("openNotifyRestClient") RestClient restClient,
            @Qualifier("positionCache") TtlCache<String, IssPosition> cache,
            @Value("${iss.cache.position-ttl-seconds}") long ttlSeconds,
            @Value("${iss.cache.max-stale-serve-seconds}") long maxStaleSeconds) {
        this.restClient = restClient;
        this.cache = cache;
        this.ttl = Duration.ofSeconds(ttlSeconds);
        this.maxStale = Duration.ofSeconds(maxStaleSeconds);
    }

    @Override
    public IssPosition getCurrentPosition() {
        return cache.getFresh(CACHE_KEY)
                .orElseGet(this::fetchLiveOrDegrade);
    }

    private IssPosition fetchLiveOrDegrade() {
        try {
            IssPosition live = fetchLive();
            cache.put(CACHE_KEY, live, ttl);
            return live;
        } catch (RestClientException e) {
            log.warn("Open-Notify call failed, attempting degraded fallback: {}", e.getMessage());
            return cache.getStaleIfWithin(CACHE_KEY, maxStale)
                    .map(stale -> stale.toBuilder().freshness(DataFreshness.CACHED_STALE).build())
                    .orElseGet(this::mockPosition);
        }
    }

    private IssPosition fetchLive() {
        OpenNotifyResponse response = restClient.get()
                .uri("/iss-now.json")
                .retrieve()
                .body(OpenNotifyResponse.class);

        if (response == null || response.issPosition() == null) {
            throw new RestClientException("Open-Notify returned an empty/unexpected body");
        }

        return IssPosition.builder()
                .latitude(Double.parseDouble(response.issPosition().latitude()))
                .longitude(Double.parseDouble(response.issPosition().longitude()))
                .altitudeKm(null)   // Open-Notify doesn't provide this
                .velocityKmh(null)  // nor this
                .observedAt(Instant.ofEpochSecond(response.timestamp()))
                .freshness(DataFreshness.LIVE)
                .build();
    }

    /** Last-resort fallback when both live call and stale cache are unavailable. */
    private IssPosition mockPosition() {
        log.warn("No stale cache available - serving mock ISS position");
        return IssPosition.builder()
                .latitude(0.0)
                .longitude(0.0)
                .altitudeKm(408.0)   // typical ISS altitude
                .velocityKmh(27600.0) // typical ISS orbital velocity
                .observedAt(Instant.now())
                .freshness(DataFreshness.MOCK)
                .build();
    }
}