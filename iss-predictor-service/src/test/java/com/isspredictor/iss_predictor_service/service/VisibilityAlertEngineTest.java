package com.isspredictor.iss_predictor_service.service;

import com.isspredictor.iss_predictor_service.model.CloudForecast;
import com.isspredictor.iss_predictor_service.model.DataFreshness;
import com.isspredictor.iss_predictor_service.model.PassPrediction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Table-driven tests for the four conditional alert rules, using the same
 * threshold values configured in application.yml (120 min visible-soon window,
 * 20% favourable cloud cover, 5 min extended duration, -2.0 bright magnitude).
 */
class VisibilityAlertEngineTest {

    private static final VisibilityAlertEngine ENGINE =
            new VisibilityAlertEngine(120, 20, 5, -2.0);

    private static final Instant NOW = Instant.parse("2026-08-09T12:00:00Z");

    private PassPrediction.PassPredictionBuilder basePass() {
        return PassPrediction.builder()
                .riseTime(NOW.plus(Duration.ofHours(1)))
                .culminationTime(NOW.plus(Duration.ofHours(1)).plus(Duration.ofMinutes(2)))
                .setTime(NOW.plus(Duration.ofHours(1)).plus(Duration.ofMinutes(4)))
                .riseAzimuthCompass("NW")
                .setAzimuthCompass("SE")
                .riseAzimuthDegrees(315.0)
                .setAzimuthDegrees(135.0)
                .maxElevationDegrees(45.0)
                .duration(Duration.ofMinutes(4))
                .magnitude(-1.0)
                .freshness(DataFreshness.LIVE);
    }

    private CloudForecast forecast(int cloudCoverPercent) {
        return CloudForecast.builder()
                .forecastHour(NOW.plus(Duration.ofHours(1)))
                .cloudCoverPercent(cloudCoverPercent)
                .freshness(DataFreshness.LIVE)
                .build();
    }

    @Nested
    @DisplayName("Rule 1: visible soon")
    class VisibleSoonRule {

        @Test
        @DisplayName("triggers when rise is within the window")
        void triggersWithinWindow() {
            PassPrediction pass = basePass().build(); // rises in 1h, window is 120min
            List<String> alerts = ENGINE.evaluate(pass, forecast(80), NOW);
            assertThat(alerts).anyMatch(a -> a.contains("ISS visible soon - Check sky in NW"));
        }

        @Test
        @DisplayName("does not trigger when rise is outside the window")
        void doesNotTriggerOutsideWindow() {
            PassPrediction pass = basePass()
                    .riseTime(NOW.plus(Duration.ofHours(3))) // outside 120min window
                    .build();
            List<String> alerts = ENGINE.evaluate(pass, forecast(80), NOW);
            assertThat(alerts).noneMatch(a -> a.contains("visible soon"));
        }

        @Test
        @DisplayName("does not trigger for a pass that has already risen")
        void doesNotTriggerForPastRise() {
            PassPrediction pass = basePass().riseTime(NOW.minus(Duration.ofMinutes(5))).build();
            List<String> alerts = ENGINE.evaluate(pass, forecast(80), NOW);
            assertThat(alerts).noneMatch(a -> a.contains("visible soon"));
        }
    }

    @Nested
    @DisplayName("Rule 2: favourable viewing conditions")
    class FavourableConditionsRule {

        @Test
        @DisplayName("triggers when visible soon AND cloud cover is under threshold")
        void triggersWhenClearAndSoon() {
            PassPrediction pass = basePass().build();
            List<String> alerts = ENGINE.evaluate(pass, forecast(15), NOW); // < 20%
            assertThat(alerts).anyMatch(a -> a.contains("Clear skies predicted"));
        }

        @Test
        @DisplayName("does not trigger when cloud cover is at or above threshold")
        void doesNotTriggerWhenCloudy() {
            PassPrediction pass = basePass().build();
            List<String> alerts = ENGINE.evaluate(pass, forecast(20), NOW); // not < 20%
            assertThat(alerts).noneMatch(a -> a.contains("Clear skies predicted"));
        }

        @Test
        @DisplayName("does not trigger when clear but not visible soon")
        void doesNotTriggerWhenClearButFarOut() {
            PassPrediction pass = basePass().riseTime(NOW.plus(Duration.ofHours(5))).build();
            List<String> alerts = ENGINE.evaluate(pass, forecast(5), NOW);
            assertThat(alerts).noneMatch(a -> a.contains("Clear skies predicted"));
        }

        @Test
        @DisplayName("does not trigger when forecast is null")
        void doesNotTriggerWhenForecastMissing() {
            PassPrediction pass = basePass().build();
            List<String> alerts = ENGINE.evaluate(pass, null, NOW);
            assertThat(alerts).noneMatch(a -> a.contains("Clear skies predicted"));
        }
    }

    @Nested
    @DisplayName("Rule 3: extended duration")
    class ExtendedDurationRule {

        @ParameterizedTest(name = "duration={0} -> triggers={1}")
        @MethodSource("durationCases")
        void evaluatesDurationThreshold(Duration duration, boolean shouldTrigger) {
            PassPrediction pass = basePass().duration(duration).build();
            List<String> alerts = ENGINE.evaluate(pass, forecast(80), NOW);
            boolean triggered = alerts.stream().anyMatch(a -> a.contains("Extended viewing"));
            assertThat(triggered).isEqualTo(shouldTrigger);
        }

        static Stream<Arguments> durationCases() {
            return Stream.of(
                    Arguments.of(Duration.ofMinutes(4), false),
                    Arguments.of(Duration.ofMinutes(5), false), // exactly 5min - not "> 5"
                    Arguments.of(Duration.ofMinutes(6), true),
                    Arguments.of(Duration.ofMinutes(10), true)
            );
        }
    }

    @Nested
    @DisplayName("Rule 4: exceptionally bright")
    class BrightnessRule {

        @ParameterizedTest(name = "magnitude={0} -> triggers={1}")
        @MethodSource("magnitudeCases")
        void evaluatesMagnitudeThreshold(Double magnitude, boolean shouldTrigger) {
            PassPrediction pass = basePass().magnitude(magnitude).build();
            List<String> alerts = ENGINE.evaluate(pass, forecast(80), NOW);
            boolean triggered = alerts.stream().anyMatch(a -> a.contains("Exceptionally bright"));
            assertThat(triggered).isEqualTo(shouldTrigger);
        }

        static Stream<Arguments> magnitudeCases() {
            return Stream.of(
                    Arguments.of(-1.0, false),
                    Arguments.of(-2.0, false), // exactly -2.0 - not "< -2.0"
                    Arguments.of(-2.5, true),
                    Arguments.of(-4.0, true),
                    Arguments.of((Double) null, false) // null magnitude never triggers
            );
        }
    }

    @Test
    @DisplayName("all four rules can trigger simultaneously for an ideal pass")
    void allRulesTriggerTogether() {
        PassPrediction idealPass = basePass()
                .duration(Duration.ofMinutes(7))
                .magnitude(-3.5)
                .build();

        List<String> alerts = ENGINE.evaluate(idealPass, forecast(10), NOW);

        assertThat(alerts).hasSize(4);
        assertThat(alerts).anyMatch(a -> a.contains("visible soon"));
        assertThat(alerts).anyMatch(a -> a.contains("Clear skies"));
        assertThat(alerts).anyMatch(a -> a.contains("Extended viewing"));
        assertThat(alerts).anyMatch(a -> a.contains("Exceptionally bright"));
    }

    @Test
    @DisplayName("no rules trigger for an unremarkable, distant pass")
    void noRulesTriggerForUnremarkablePass() {
        PassPrediction dullPass = basePass()
                .riseTime(NOW.plus(Duration.ofHours(10)))
                .duration(Duration.ofMinutes(2))
                .magnitude(2.0)
                .build();

        List<String> alerts = ENGINE.evaluate(dullPass, forecast(90), NOW);

        assertThat(alerts).isEmpty();
    }
}