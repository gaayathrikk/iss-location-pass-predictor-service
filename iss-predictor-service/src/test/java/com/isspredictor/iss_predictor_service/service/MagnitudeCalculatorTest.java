package com.isspredictor.iss_predictor_service.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class MagnitudeCalculatorTest {

    private final MagnitudeCalculator calculator = new MagnitudeCalculator();

    @ParameterizedTest(name = "elevation={0} -> magnitude ~= {1}")
    @CsvSource({
            "90.0, -4.0",   // overhead pass -> brightest
            "0.0, 0.5",     // horizon pass -> dimmest
            "45.0, -1.75"   // halfway -> linear midpoint between -4.0 and 0.5
    })
    void interpolatesLinearlyBetweenDimmestAndBrightest(double elevation, double expectedMagnitude) {
        double result = calculator.estimateMagnitude(elevation);
        assertThat(result).isCloseTo(expectedMagnitude, within(0.01));
    }

    @Test
    @DisplayName("clamps elevation above 90 degrees to the brightest value")
    void clampsAboveNinety() {
        assertThat(calculator.estimateMagnitude(120.0))
                .isEqualTo(calculator.estimateMagnitude(90.0));
    }

    @Test
    void clampsBelowZero() {
        assertThat(calculator.estimateMagnitude(-10.0))
                .isEqualTo(calculator.estimateMagnitude(0.0));
    }

    @Test
    void higherElevationIsAlwaysAtLeastAsBright() {
        double lower = calculator.estimateMagnitude(30.0);
        double higher = calculator.estimateMagnitude(60.0);
        // lower magnitude number = brighter, so higher elevation should give a smaller value
        assertThat(higher).isLessThanOrEqualTo(lower);
    }
}