package com.isspredictor.iss_predictor_service.client;

import com.isspredictor.iss_predictor_service.cache.TtlCache;
import com.isspredictor.iss_predictor_service.client.dto.OpenMeteoResponse;
import com.isspredictor.iss_predictor_service.model.CloudForecast;
import com.isspredictor.iss_predictor_service.model.Coordinates;
import com.isspredictor.iss_predictor_service.model.DataFreshness;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Implements {@link WeatherClient} against Open-Meteo's hourly forecast endpoint.
 * <p>
 * Open-Meteo returns two parallel arrays (times, cloud_cover%) rather than a
 * list of objects - {@link #toCloudForecasts} is where that pairing-by-index
 * quirk gets absorbed, so nothing outside this client ever deals with it.
 */
@Component
@Slf4j
public class OpenMeteoClient implements WeatherClient {

    private static final DateTimeFormatter OPEN_METEO_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    private final RestClient restClient;
    private final TtlCache<String, CloudForecast> cache;
    private final Duration ttl;
    private final Duration maxStale;

    public OpenMeteoClient(
            @Qualifier("openMeteoRestClient") RestClient restClient,
            @Qualifier("weatherCache") TtlCache<String, CloudForecast> cache,
            @Value("${iss.cache.weather-ttl-seconds}") long ttlSeconds,
            @Value("${iss.cache.max-stale-serve-seconds}") long maxStaleSeconds) {
        this.restClient = restClient;
        this.cache = cache;
        this.ttl = Duration.ofSeconds(ttlSeconds);
        this.maxStale = Duration.ofSeconds(maxStaleSeconds);
    }

    @Override
    public CloudForecast getCloudForecast(Coordinates location, Instant atTime) {
        String key = cacheKey(location, atTime);
        return cache.getFresh(key)
                .orElseGet(() -> fetchLiveOrDegrade(location, atTime, key));
    }

    private CloudForecast fetchLiveOrDegrade(Coordinates location, Instant atTime, String key) {
        try {
            CloudForecast live = fetchLive(location, atTime);
            cache.put(key, live, ttl);
            return live;
        } catch (RestClientException e) {
            log.warn("Open-Meteo call failed for {}, attempting degraded fallback: {}", key, e.getMessage());
            return cache.getStaleIfWithin(key, maxStale)
                    .map(stale -> stale.toBuilder().freshness(DataFreshness.CACHED_STALE).build())
                    .orElseGet(() -> mockForecast(atTime));
        }
    }

    private CloudForecast fetchLive(Coordinates location, Instant atTime) {
        OpenMeteoResponse response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1/forecast")
                        .queryParam("latitude", location.latitude())
                        .queryParam("longitude", location.longitude())
                        .queryParam("hourly", "cloud_cover")
                        .build())
                .retrieve()
                .body(OpenMeteoResponse.class);

        if (response == null || response.hourly() == null) {
            throw new RestClientException("Open-Meteo returned an empty/unexpected body");
        }

        return findClosestHour(response.hourly(), atTime)
                .orElseThrow(() -> new RestClientException("No forecast hour found near " + atTime));
    }

    /** Pairs the two parallel arrays by index and finds the entry closest to atTime. */
    private java.util.Optional<CloudForecast> findClosestHour(OpenMeteoResponse.Hourly hourly, Instant atTime) {
        List<String> times = hourly.time();
        List<Integer> cloudCover = hourly.cloudCover();

        CloudForecast closest = null;
        long smallestDiff = Long.MAX_VALUE;

        for (int i = 0; i < times.size() && i < cloudCover.size(); i++) {
            Instant hourInstant = LocalDateTime.parse(times.get(i), OPEN_METEO_TIME_FORMAT)
                    .toInstant(ZoneOffset.UTC);
            long diff = Math.abs(Duration.between(atTime, hourInstant).toSeconds());
            if (diff < smallestDiff) {
                smallestDiff = diff;
                closest = CloudForecast.builder()
                        .forecastHour(hourInstant)
                        .cloudCoverPercent(cloudCover.get(i))
                        .freshness(DataFreshness.LIVE)
                        .build();
            }
        }
        return java.util.Optional.ofNullable(closest);
    }

    private String cacheKey(Coordinates location, Instant atTime) {
        return String.format(Locale.ROOT, "weather:%.2f,%.2f:%s",
                location.latitude(), location.longitude(), atTime.truncatedTo(java.time.temporal.ChronoUnit.HOURS));
    }

    /** Last-resort fallback: assume moderate, non-blocking cloud cover. */
    private CloudForecast mockForecast(Instant atTime) {
        log.warn("No stale cache available - serving mock cloud forecast");
        return CloudForecast.builder()
                .forecastHour(atTime)
                .cloudCoverPercent(50)
                .freshness(DataFreshness.MOCK)
                .build();
    }
}