package com.isspredictor.iss_predictor_service.client;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.isspredictor.iss_predictor_service.cache.TtlCache;
import com.isspredictor.iss_predictor_service.model.DataFreshness;
import com.isspredictor.iss_predictor_service.model.IssPosition;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.time.Duration;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises the full Option A degradation chain against a real (WireMock)
 * HTTP server: live success, cache-hit short-circuit, live failure -> stale
 * cache, live failure with no stale -> mock. Each test builds its own client
 * instance directly (no Spring context) - the constructor is plain, so this
 * is faster and more focused than a full @SpringBootTest.
 */
class OpenNotifyClientTest {

    private WireMockServer wireMock;
    private TtlCache<String, IssPosition> cache;
    private OpenNotifyClient client;

    private static final String LIVE_RESPONSE = """
            {
              "message": "success",
              "timestamp": 1690000000,
              "iss_position": { "latitude": "12.34", "longitude": "56.78" }
            }
            """;

    @BeforeEach
    void setUp() {
        wireMock = new WireMockServer(options().dynamicPort());
        wireMock.start();

        RestClient restClient = RestClient.builder().baseUrl(wireMock.baseUrl()).build();
        cache = new TtlCache<>();
        // ttl=5s, maxStale=6h - same values as application.yml defaults
        client = new OpenNotifyClient(restClient, cache, 5, 21_600);
    }

    @AfterEach
    void tearDown() {
        wireMock.stop();
    }

    @Test
    void returnsLivePositionOnSuccessfulCall() {
        wireMock.stubFor(get(urlPathEqualTo("/iss-now.json"))
                .willReturn(aResponse().withHeader("Content-Type", "application/json").withBody(LIVE_RESPONSE)));

        IssPosition position = client.getCurrentPosition();

        assertThat(position.getLatitude()).isEqualTo(12.34);
        assertThat(position.getLongitude()).isEqualTo(56.78);
        assertThat(position.getFreshness()).isEqualTo(DataFreshness.LIVE);
    }

    @Test
    void secondCallWithinTtlDoesNotHitTheNetworkAgain() {
        wireMock.stubFor(get(urlPathEqualTo("/iss-now.json"))
                .willReturn(aResponse().withHeader("Content-Type", "application/json").withBody(LIVE_RESPONSE)));

        client.getCurrentPosition(); // first call - populates cache
        client.getCurrentPosition(); // second call - should be served from cache

        wireMock.verify(1, getRequestedFor(urlPathEqualTo("/iss-now.json")));
    }

    @Test
    void fallsBackToStaleCacheWhenLiveCallFails() {
        // First call succeeds and populates the cache with a very short TTL,
        // so the second call finds it expired-but-still-stale-serveable.
        RestClient restClient = RestClient.builder().baseUrl(wireMock.baseUrl()).build();
        OpenNotifyClient shortTtlClient = new OpenNotifyClient(restClient, cache, 0, 21_600);

        wireMock.stubFor(get(urlPathEqualTo("/iss-now.json"))
                .willReturn(aResponse().withHeader("Content-Type", "application/json").withBody(LIVE_RESPONSE)));
        shortTtlClient.getCurrentPosition(); // populates cache, immediately "expired" (ttl=0)

        wireMock.stubFor(get(urlPathEqualTo("/iss-now.json")).willReturn(aResponse().withStatus(500)));

        IssPosition result = shortTtlClient.getCurrentPosition();

        assertThat(result.getLatitude()).isEqualTo(12.34); // same data as the original live call
        assertThat(result.getFreshness()).isEqualTo(DataFreshness.CACHED_STALE);
    }

    @Test
    void fallsBackToMockWhenLiveCallFailsAndNoStaleCacheExists() {
        wireMock.stubFor(get(urlPathEqualTo("/iss-now.json")).willReturn(aResponse().withStatus(500)));

        IssPosition result = client.getCurrentPosition();

        assertThat(result.getFreshness()).isEqualTo(DataFreshness.MOCK);
        assertThat(result.getAltitudeKm()).isEqualTo(408.0); // the hardcoded mock value
    }

    @Test
    void treatsUnexpectedResponseShapeAsAFailure() {
        wireMock.stubFor(get(urlPathEqualTo("/iss-now.json"))
                .willReturn(aResponse().withHeader("Content-Type", "application/json").withBody("{}")));

        IssPosition result = client.getCurrentPosition();

        // no iss_position field -> our client's null-check throws -> degrades to mock
        assertThat(result.getFreshness()).isEqualTo(DataFreshness.MOCK);
    }
}