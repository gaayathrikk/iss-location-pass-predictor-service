package com.isspredictor.iss_predictor_service.service;

import com.isspredictor.iss_predictor_service.model.CloudForecast;
import com.isspredictor.iss_predictor_service.model.PassPrediction;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Evaluates the four conditional alert rules from the problem statement
 * against a single pass (and its associated cloud forecast). Rules are
 * independent and additive - a single pass can trigger 0 to 4 alerts.
 */
@Component
public class VisibilityAlertEngine {

    private final Duration visibleSoonWindow;
    private final int favourableCloudCoverPct;
    private final Duration extendedDurationThreshold;
    private final double brightMagnitudeThreshold;

    public VisibilityAlertEngine(
            @Value("${iss.alerts.visible-soon-window-minutes}") long visibleSoonWindowMinutes,
            @Value("${iss.alerts.favourable-cloud-cover-pct}") int favourableCloudCoverPct,
            @Value("${iss.alerts.extended-duration-minutes}") long extendedDurationMinutes,
            @Value("${iss.alerts.bright-magnitude-threshold}") double brightMagnitudeThreshold) {
        this.visibleSoonWindow = Duration.ofMinutes(visibleSoonWindowMinutes);
        this.favourableCloudCoverPct = favourableCloudCoverPct;
        this.extendedDurationThreshold = Duration.ofMinutes(extendedDurationMinutes);
        this.brightMagnitudeThreshold = brightMagnitudeThreshold;
    }

    /**
     * @param pass the pass to evaluate
     * @param cloudForecast cloud cover at the pass's rise time, or null if unavailable
     * @param now current time, injected for testability rather than calling Instant.now() inline
     */
    public List<String> evaluate(PassPrediction pass, CloudForecast cloudForecast, Instant now) {
        List<String> alerts = new ArrayList<>();

        boolean visibleSoon = isVisibleSoon(pass, now);
        if (visibleSoon) {
            alerts.add("ISS visible soon - Check sky in " + pass.getRiseAzimuthCompass());
        }

        if (visibleSoon && cloudForecast != null && cloudForecast.isFavourable(favourableCloudCoverPct)) {
            alerts.add("Clear skies predicted - Excellent viewing opportunity");
        }

        if (pass.exceedsDuration(extendedDurationThreshold)) {
            alerts.add("Extended viewing opportunity");
        }

        if (pass.isBrighterThan(brightMagnitudeThreshold)) {
            alerts.add("Exceptionally bright pass - Easily visible");
        }

        return alerts;
    }

    private boolean isVisibleSoon(PassPrediction pass, Instant now) {
        if (pass.getRiseTime() == null || pass.getRiseTime().isBefore(now)) {
            return false;
        }
        return Duration.between(now, pass.getRiseTime()).compareTo(visibleSoonWindow) <= 0;
    }
}