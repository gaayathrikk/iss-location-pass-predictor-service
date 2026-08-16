package com.isspredictor.iss_predictor_service.util;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class AzimuthFormatterTest {

    @ParameterizedTest(name = "{0} degrees -> {1}")
    @CsvSource({
            "0,   N",
            "45,  NE",
            "90,  E",
            "135, SE",
            "180, S",
            "225, SW",
            "270, W",
            "315, NW",
            "360, N",    // full circle wraps back to N
            "359, N",    // just under 360, rounds up to N (0/360)
            "22,  N",    // just under the NE boundary
            "23,  NE",   // just over the NE boundary
            "-45, NW",   // negative input, handled via the modulo normalization
            "-90, W"
    })
    void convertsDegreesToCompassPoint(double degrees, String expected) {
        assertThat(AzimuthFormatter.toCompass(degrees)).isEqualTo(expected);
    }
}