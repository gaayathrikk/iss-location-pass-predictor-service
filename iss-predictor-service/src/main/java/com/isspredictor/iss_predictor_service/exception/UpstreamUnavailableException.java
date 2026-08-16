package com.isspredictor.iss_predictor_service.exception;

/** Signals a single upstream API call failed. Caught internally by client implementations - never expected to escape to the controller layer. */
public class UpstreamUnavailableException extends RuntimeException {
    public UpstreamUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}