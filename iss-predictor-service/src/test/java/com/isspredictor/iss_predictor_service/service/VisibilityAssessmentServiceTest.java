package com.isspredictor.iss_predictor_service.service;

import com.isspredictor.iss_predictor_service.client.IssPositionClient;
import com.isspredictor.iss_predictor_service.client.PassPredictionClient;
import com.isspredictor.iss_predictor_service.client.WeatherClient;
import com.isspredictor.iss_predictor_service.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class VisibilityAssessmentServiceTest {

    @Mock private IssPositionClient positionClient;
    @Mock private PassPredictionClient passClient;
    @Mock private WeatherClient weatherClient;

    private VisibilityAssessmentService service;
    private static final Coordinates BENGALURU = new Coordinates(12.97, 77.59);

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        VisibilityAlertEngine alertEngine = new VisibilityAlertEngine(120, 20, 5, -2.0);
        MagnitudeCalculator magnitudeCalculator = new MagnitudeCalculator();
        service = new VisibilityAssessmentService(
                positionClient, passClient, weatherClient, alertEngine, magnitudeCalculator);
    }

    private IssPosition livePosition() {
        return IssPosition.builder()
                .latitude(25.4).longitude(88.1)
                .altitudeKm(408.0).velocityKmh(27600.0)
                .observedAt(Instant.now())
                .freshness(DataFreshness.LIVE)
                .build();
    }

    private PassPrediction pass(DataFreshness freshness) {
        return PassPrediction.builder()
                .riseTime(Instant.now().plus(Duration.ofMinutes(30)))
                .culminationTime(Instant.now().plus(Duration.ofMinutes(32)))
                .setTime(Instant.now().plus(Duration.ofMinutes(35)))
                .riseAzimuthCompass("NW").setAzimuthCompass("SE")
                .riseAzimuthDegrees(315.0).setAzimuthDegrees(135.0)
                .maxElevationDegrees(70.0) // high elevation -> should get a bright estimated magnitude
                .duration(Duration.ofMinutes(5))
                .freshness(freshness)
                .build(); // magnitude deliberately left null - service should fill it in
    }

    private CloudForecast forecast(DataFreshness freshness) {
        return CloudForecast.builder()
                .forecastHour(Instant.now())
                .cloudCoverPercent(10)
                .freshness(freshness)
                .build();
    }

    @Test
    void assemblesACompleteAssessmentFromAllThreeClients() {
        when(positionClient.getCurrentPosition()).thenReturn(livePosition());
        when(passClient.getUpcomingPasses(any())).thenReturn(List.of(pass(DataFreshness.LIVE)));
        when(weatherClient.getCloudForecast(any(), any())).thenReturn(forecast(DataFreshness.LIVE));

        VisibilityAssessment result = service.assess(BENGALURU);

        assertThat(result.getLocation()).isEqualTo(BENGALURU);
        assertThat(result.getCurrentPosition().getLatitude()).isEqualTo(25.4);
        assertThat(result.getUpcomingPasses()).hasSize(1);
        assertThat(result.getOverallFreshness()).isEqualTo(DataFreshness.LIVE);
    }

    @Test
    void fillsInMagnitudeUsingTheMagnitudeCalculator() {
        when(positionClient.getCurrentPosition()).thenReturn(livePosition());
        when(passClient.getUpcomingPasses(any())).thenReturn(List.of(pass(DataFreshness.LIVE)));
        when(weatherClient.getCloudForecast(any(), any())).thenReturn(forecast(DataFreshness.LIVE));

        VisibilityAssessment result = service.assess(BENGALURU);

        // pass builder deliberately left magnitude null - service must compute it
        PassPrediction enriched = result.getUpcomingPasses().get(0);
        assertThat(enriched.getMagnitude()).isNotNull();
        assertThat(enriched.getMagnitude()).isLessThan(0); // 70deg elevation should be reasonably bright
    }

    @Test
    void generatesAlertsFromTheAlertEngine() {
        when(positionClient.getCurrentPosition()).thenReturn(livePosition());
        when(passClient.getUpcomingPasses(any())).thenReturn(List.of(pass(DataFreshness.LIVE)));
        when(weatherClient.getCloudForecast(any(), any())).thenReturn(forecast(DataFreshness.LIVE)); // 10% cloud, favourable

        VisibilityAssessment result = service.assess(BENGALURU);

        // rise in 30min (within 120min window) + clear skies (10% < 20%) should trigger at least these two
        assertThat(result.getAlerts()).anyMatch(a -> a.contains("visible soon"));
        assertThat(result.getAlerts()).anyMatch(a -> a.contains("Clear skies"));
    }

    @Test
    void overallFreshnessReflectsTheWorstSourceWhenPositionIsStale() {
        when(positionClient.getCurrentPosition()).thenReturn(
                livePosition().toBuilder().freshness(DataFreshness.CACHED_STALE).build());
        when(passClient.getUpcomingPasses(any())).thenReturn(List.of(pass(DataFreshness.LIVE)));
        when(weatherClient.getCloudForecast(any(), any())).thenReturn(forecast(DataFreshness.LIVE));

        VisibilityAssessment result = service.assess(BENGALURU);

        assertThat(result.getOverallFreshness()).isEqualTo(DataFreshness.CACHED_STALE);
    }

    @Test
    void overallFreshnessReflectsMockEvenWhenOnlyOneSourceIsMocked() {
        when(positionClient.getCurrentPosition()).thenReturn(livePosition()); // LIVE
        when(passClient.getUpcomingPasses(any())).thenReturn(List.of(pass(DataFreshness.LIVE))); // LIVE
        when(weatherClient.getCloudForecast(any(), any())).thenReturn(forecast(DataFreshness.MOCK)); // MOCK

        VisibilityAssessment result = service.assess(BENGALURU);

        // MOCK is worse than LIVE, so it should "win" the worst-of computation even though
        // it's only one out of three sources
        assertThat(result.getOverallFreshness()).isEqualTo(DataFreshness.MOCK);
    }

    @Test
    void handlesNoUpcomingPassesGracefully() {
        when(positionClient.getCurrentPosition()).thenReturn(livePosition());
        when(passClient.getUpcomingPasses(any())).thenReturn(List.of()); // no passes predicted
        // weatherClient should never even be called since there's no pass to look up weather for

        VisibilityAssessment result = service.assess(BENGALURU);

        assertThat(result.getUpcomingPasses()).isEmpty();
        assertThat(result.getAlerts()).isEmpty();
    }
}