package com.isspredictor.iss_predictor_service.model;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

/**
 * Validated geographic coordinate supplied by the end user.
 * <p>
 * Kept as its own type (rather than two raw doubles floating around every
 * method signature) so validation lives in exactly one place and every
 * downstream class can trust the values are in range.
 */
public record Coordinates(

        @NotNull
        @DecimalMin(value = "-90.0", message = "Latitude must be >= -90")
        @DecimalMax(value = "90.0", message = "Latitude must be <= 90")
        Double latitude,

        @NotNull
        @DecimalMin(value = "-180.0", message = "Longitude must be >= -180")
        @DecimalMax(value = "180.0", message = "Longitude must be <= 180")
        Double longitude
) {
}