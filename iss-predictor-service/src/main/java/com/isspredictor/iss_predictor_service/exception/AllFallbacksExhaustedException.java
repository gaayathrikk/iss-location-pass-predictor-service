package com.isspredictor.iss_predictor_service.exception;

/** Signals live call, stale cache, AND mock fallback all failed for a source - the true worst case, mapped to 503 if it's ever actually thrown. */
public class AllFallbacksExhaustedException extends RuntimeException {
    public AllFallbacksExhaustedException(String message) {
        super(message);
    }
}