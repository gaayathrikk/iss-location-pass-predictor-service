package com.isspredictor.iss_predictor_service.model;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.Map;

/**
 * Cloud cover forecast for a single hour, used to decide whether a pass
 * will actually be visible (a bright, well-timed pass is still useless
 * if the sky is overcast).
 */
@Value
@Builder(toBuilder = true)
public class CloudForecast {

    Instant forecastHour;

    //  0-100
    int cloudCoverPercent;

    DataFreshness freshness;

    public boolean isFavourable(int thresholdPercent) {
        return cloudCoverPercent < thresholdPercent;
    }
}