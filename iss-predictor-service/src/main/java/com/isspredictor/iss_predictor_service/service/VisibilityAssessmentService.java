package com.isspredictor.iss_predictor_service.service;

import com.isspredictor.iss_predictor_service.client.IssPositionClient;
import com.isspredictor.iss_predictor_service.client.PassPredictionClient;
import com.isspredictor.iss_predictor_service.client.WeatherClient;
import com.isspredictor.iss_predictor_service.model.*;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Orchestrates the three upstream clients and the alert/magnitude logic into
 * one aggregated {@link VisibilityAssessment}.
 * <p>
 * The three client calls run concurrently on virtual threads (Java 21) rather
 * than sequentially - since none depend on each other's results, this cuts
 * total latency roughly to the slowest single call instead of the sum of all
 * three, which matters given each has its own multi-second timeout budget.
 */
@Service
public class VisibilityAssessmentService {

    private final IssPositionClient positionClient;
    private final PassPredictionClient passClient;
    private final WeatherClient weatherClient;
    private final VisibilityAlertEngine alertEngine;
    private final MagnitudeCalculator magnitudeCalculator;

    public VisibilityAssessmentService(
            IssPositionClient positionClient,
            PassPredictionClient passClient,
            WeatherClient weatherClient,
            VisibilityAlertEngine alertEngine,
            MagnitudeCalculator magnitudeCalculator) {
        this.positionClient = positionClient;
        this.passClient = passClient;
        this.weatherClient = weatherClient;
        this.alertEngine = alertEngine;
        this.magnitudeCalculator = magnitudeCalculator;
    }

    public VisibilityAssessment assess(Coordinates location) {
        Instant now = Instant.now();

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<IssPosition> positionFuture = executor.submit(positionClient::getCurrentPosition);
            Future<List<PassPrediction>> passesFuture = executor.submit(() -> passClient.getUpcomingPasses(location));

            IssPosition position = await(positionFuture);
            List<PassPrediction> rawPasses = await(passesFuture);

            List<PassPrediction> enrichedPasses = enrichWithMagnitude(rawPasses);
            List<String> allAlerts = new ArrayList<>();
            DataFreshness overallFreshness = position.getFreshness();

            for (PassPrediction pass : enrichedPasses) {
                CloudForecast forecast = weatherClient.getCloudForecast(location, pass.getRiseTime());
                allAlerts.addAll(alertEngine.evaluate(pass, forecast, now));
                overallFreshness = worstOf(overallFreshness, pass.getFreshness());
                overallFreshness = worstOf(overallFreshness, forecast.getFreshness());
            }

            return VisibilityAssessment.builder()
                    .location(location)
                    .currentPosition(position)
                    .upcomingPasses(enrichedPasses)
                    .alerts(allAlerts)
                    .overallFreshness(overallFreshness)
                    .build();
        }
    }

    private List<PassPrediction> enrichWithMagnitude(List<PassPrediction> passes) {
        return passes.stream()
                .map(p -> p.toBuilder()
                        .magnitude(magnitudeCalculator.estimateMagnitude(p.getMaxElevationDegrees()))
                        .build())
                .toList();
    }

    /** Freshness "worst-of": MOCK is worse than CACHED_STALE is worse than LIVE. */
    private DataFreshness worstOf(DataFreshness a, DataFreshness b) {
        return a.ordinal() >= b.ordinal() ? a : b;
    }

    private <T> T await(Future<T> future) {
        try {
            return future.get();
        } catch (Exception e) {
            throw new IllegalStateException("Upstream call failed unexpectedly", e);
        }
    }
}