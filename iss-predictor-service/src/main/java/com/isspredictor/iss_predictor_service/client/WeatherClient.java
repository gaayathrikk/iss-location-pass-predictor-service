package com.isspredictor.iss_predictor_service.client;

import com.isspredictor.iss_predictor_service.model.CloudForecast;
import com.isspredictor.iss_predictor_service.model.Coordinates;

import java.time.Instant;

public interface WeatherClient {

    /**
     * Returns the cloud cover forecast for the hour containing {@code atTime}.
     * Never throws for upstream failures - falls back per Option A strategy.
     */
    CloudForecast getCloudForecast(Coordinates location, Instant atTime);
}