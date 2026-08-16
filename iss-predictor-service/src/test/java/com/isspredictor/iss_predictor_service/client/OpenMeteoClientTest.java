package com.isspredictor.iss_predictor_service.client;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.isspredictor.iss_predictor_service.cache.TtlCache;
import com.isspredictor.iss_predictor_service.model.CloudForecast;
import com.isspredictor.iss_predictor_service.model.Coordinates;
import com.isspredictor.iss_predictor_service.model.DataFreshness;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.time.Instant;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;

class OpenMeteoClientTest {

    private WireMockServer wireMock;
    private TtlCache<String, CloudForecast> cache;
    private OpenMeteoClient client;

    private static final Coordinates BENGALURU = new Coordinates(12.97, 77.59);

    private static final String LIVE_RESPONSE = """
            {
              "hourly": {
                "time": ["2026-08-09T10:00", "2026-08-09T11:00", "2026-08-09T12:00"],
                "cloud_cover": [40, 15, 90]
              }
            }
            """;

    @BeforeEach
    void setUp() {
        wireMock = new WireMockServer(options().dynamicPort());
        wireMock.start();

        RestClient restClient = RestClient.builder().baseUrl(wireMock.baseUrl()).build();
        cache = new TtlCache<>();
        client = new OpenMeteoClient(restClient, cache, 900, 21_600);
    }

    @AfterEach
    void tearDown() {
        wireMock.stop();
    }

    @Test
    void picksTheHourClosestToTheRequestedTime() {
        wireMock.stubFor(get(urlPathEqualTo("/v1/forecast"))
                .willReturn(aResponse().withHeader("Content-Type", "application/json").withBody(LIVE_RESPONSE)));

        // requesting exactly the "11:00" slot - should pair with cloud_cover[1] = 15
        CloudForecast forecast = client.getCloudForecast(BENGALURU, Instant.parse("2026-08-09T11:00:00Z"));

        assertThat(forecast.getCloudCoverPercent()).isEqualTo(15);
        assertThat(forecast.getFreshness()).isEqualTo(DataFreshness.LIVE);
    }

    @Test
    void picksTheNearestHourWhenRequestedTimeFallsBetweenSlots() {
        wireMock.stubFor(get(urlPathEqualTo("/v1/forecast"))
                .willReturn(aResponse().withHeader("Content-Type", "application/json").withBody(LIVE_RESPONSE)));

        // 11:50 is closer to the 12:00 slot (10 min away) than the 11:00 slot (50 min away)
        CloudForecast forecast = client.getCloudForecast(BENGALURU, Instant.parse("2026-08-09T11:50:00Z"));

        assertThat(forecast.getCloudCoverPercent()).isEqualTo(90); // the 12:00 slot's value
    }

    @Test
    void fallsBackToStaleCacheWhenLiveCallFails() {
        RestClient restClient = RestClient.builder().baseUrl(wireMock.baseUrl()).build();
        OpenMeteoClient shortTtlClient = new OpenMeteoClient(restClient, cache, 0, 21_600);

        wireMock.stubFor(get(urlPathEqualTo("/v1/forecast"))
                .willReturn(aResponse().withHeader("Content-Type", "application/json").withBody(LIVE_RESPONSE)));
        Instant target = Instant.parse("2026-08-09T11:00:00Z");
        shortTtlClient.getCloudForecast(BENGALURU, target);

        wireMock.stubFor(get(urlPathEqualTo("/v1/forecast")).willReturn(aResponse().withStatus(500)));

        CloudForecast result = shortTtlClient.getCloudForecast(BENGALURU, target);

        assertThat(result.getCloudCoverPercent()).isEqualTo(15);
        assertThat(result.getFreshness()).isEqualTo(DataFreshness.CACHED_STALE);
    }

    @Test
    void fallsBackToMockWhenLiveCallFailsAndNoStaleCacheExists() {
        wireMock.stubFor(get(urlPathEqualTo("/v1/forecast")).willReturn(aResponse().withStatus(500)));

        CloudForecast result = client.getCloudForecast(BENGALURU, Instant.parse("2026-08-09T11:00:00Z"));

        assertThat(result.getFreshness()).isEqualTo(DataFreshness.MOCK);
        assertThat(result.getCloudCoverPercent()).isEqualTo(50); // the hardcoded "moderate" mock value
    }
}