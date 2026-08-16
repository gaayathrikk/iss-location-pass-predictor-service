package com.isspredictor.iss_predictor_service.client;

import com.isspredictor.iss_predictor_service.model.Coordinates;
import com.isspredictor.iss_predictor_service.model.PassPrediction;

import java.util.List;

public interface PassPredictionClient {

    /**
     * Never throws for upstream failures - returns the best available list
     * (live, stale-cached, or mock), each entry tagged with its own freshness.
     * May return an empty list if no passes are predicted in the forecast window.
     */
    List<PassPrediction> getUpcomingPasses(Coordinates location);
}