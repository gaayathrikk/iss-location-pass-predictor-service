package com.isspredictor.iss_predictor_service.client;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.isspredictor.iss_predictor_service.cache.TtlCache;
import com.isspredictor.iss_predictor_service.model.Coordinates;
import com.isspredictor.iss_predictor_service.model.DataFreshness;
import com.isspredictor.iss_predictor_service.model.PassPrediction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;

class PolluxLabsClientTest {

    private WireMockServer wireMock;
    private TtlCache<String, List<PassPrediction>> cache;
    private PolluxLabsClient client;

    private static final Coordinates BENGALURU = new Coordinates(12.97, 77.59);

    private static final String LIVE_RESPONSE = """
            {
              "passes": [
                {
                  "rise_time": 1690000000,
                  "set_time": 1690000360,
                  "culmination_time": 1690000180,
                  "duration_seconds": 360,
                  "max_elevation_deg": 62.0,
                  "rise_azimuth_deg": 315.0,
                  "set_azimuth_deg": 135.0
                }
              ]
            }
            """;

    @BeforeEach
    void setUp() {
        wireMock = new WireMockServer(options().dynamicPort());
        wireMock.start();

        RestClient restClient = RestClient.builder().baseUrl(wireMock.baseUrl()).build();
        cache = new TtlCache<>();
        client = new PolluxLabsClient(restClient, cache, 1800, 21_600);
    }

    @AfterEach
    void tearDown() {
        wireMock.stop();
    }

    @Test
    void returnsLivePassesOnSuccessfulCall() {
        wireMock.stubFor(get(urlPathEqualTo("/iss-pass"))
                .willReturn(aResponse().withHeader("Content-Type", "application/json").withBody(LIVE_RESPONSE)));

        List<PassPrediction> passes = client.getUpcomingPasses(BENGALURU);

        assertThat(passes).hasSize(1);
        PassPrediction pass = passes.get(0);
        assertThat(pass.getMaxElevationDegrees()).isEqualTo(62.0);
        assertThat(pass.getRiseAzimuthCompass()).isEqualTo("NW"); // 315 deg -> NW, via AzimuthFormatter
        assertThat(pass.getSetAzimuthCompass()).isEqualTo("SE");  // 135 deg -> SE
        assertThat(pass.getFreshness()).isEqualTo(DataFreshness.LIVE);
        assertThat(pass.getMagnitude()).isNull(); // deliberately left for the service layer to fill in
    }

    @Test
    void requestIncludesLatAndLonAsQueryParams() {
        wireMock.stubFor(get(urlPathEqualTo("/iss-pass"))
                .willReturn(aResponse().withHeader("Content-Type", "application/json").withBody(LIVE_RESPONSE)));

        client.getUpcomingPasses(BENGALURU);

        wireMock.verify(getRequestedFor(urlPathEqualTo("/iss-pass"))
                .withQueryParam("lat", equalTo("12.97"))
                .withQueryParam("lon", equalTo("77.59")));
    }

    @Test
    void fallsBackToStaleCacheWhenLiveCallFails() {
        RestClient restClient = RestClient.builder().baseUrl(wireMock.baseUrl()).build();
        PolluxLabsClient shortTtlClient = new PolluxLabsClient(restClient, cache, 0, 21_600);

        wireMock.stubFor(get(urlPathEqualTo("/iss-pass"))
                .willReturn(aResponse().withHeader("Content-Type", "application/json").withBody(LIVE_RESPONSE)));
        shortTtlClient.getUpcomingPasses(BENGALURU); // populates cache, immediately expired

        wireMock.stubFor(get(urlPathEqualTo("/iss-pass")).willReturn(aResponse().withStatus(503)));

        List<PassPrediction> result = shortTtlClient.getUpcomingPasses(BENGALURU);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getMaxElevationDegrees()).isEqualTo(62.0); // same data, just relabeled
        assertThat(result.get(0).getFreshness()).isEqualTo(DataFreshness.CACHED_STALE);
    }

    @Test
    void fallsBackToMockWhenLiveCallFailsAndNoStaleCacheExists() {
        wireMock.stubFor(get(urlPathEqualTo("/iss-pass")).willReturn(aResponse().withStatus(503)));

        List<PassPrediction> result = client.getUpcomingPasses(BENGALURU);

        assertThat(result).hasSize(1); // the hardcoded single mock pass
        assertThat(result.get(0).getFreshness()).isEqualTo(DataFreshness.MOCK);
    }

    @Test
    void treatsMissingPassesFieldAsAFailure() {
        wireMock.stubFor(get(urlPathEqualTo("/iss-pass"))
                .willReturn(aResponse().withHeader("Content-Type", "application/json").withBody("{}")));

        List<PassPrediction> result = client.getUpcomingPasses(BENGALURU);

        assertThat(result.get(0).getFreshness()).isEqualTo(DataFreshness.MOCK);
    }
}