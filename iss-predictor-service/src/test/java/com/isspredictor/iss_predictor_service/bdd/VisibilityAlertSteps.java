package com.isspredictor.iss_predictor_service.bdd;

import com.isspredictor.iss_predictor_service.model.CloudForecast;
import com.isspredictor.iss_predictor_service.model.DataFreshness;
import com.isspredictor.iss_predictor_service.model.PassPrediction;
import com.isspredictor.iss_predictor_service.service.VisibilityAlertEngine;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * No Spring context needed here - VisibilityAlertEngine's constructor takes
 * only primitive thresholds, same reasoning as VisibilityAlertEngineTest.
 * Cucumber creates a fresh instance of this class per scenario, so instance
 * fields are naturally reset between scenarios with no manual cleanup needed.
 */
public class VisibilityAlertSteps {

    private VisibilityAlertEngine engine;
    private Instant now;
    private PassPrediction.PassPredictionBuilder passBuilder;
    private CloudForecast forecast;
    private List<String> alerts;

    @Given("the visibility alert engine is configured with a {int} minute visible-soon window, {int}% favourable cloud cover threshold, {int} minute extended duration threshold, and {double} bright magnitude threshold")
    public void configureEngine(int visibleSoonMinutes, int cloudCoverPct, int durationMinutes, double magnitudeThreshold) {
        engine = new VisibilityAlertEngine(visibleSoonMinutes, cloudCoverPct, durationMinutes, magnitudeThreshold);
        now = Instant.parse("2026-08-09T12:00:00Z");
        passBuilder = defaultPass();
    }

    private PassPrediction.PassPredictionBuilder defaultPass() {
        return PassPrediction.builder()
                .riseTime(now.plus(Duration.ofMinutes(60)))
                .culminationTime(now.plus(Duration.ofMinutes(62)))
                .setTime(now.plus(Duration.ofMinutes(65)))
                .riseAzimuthCompass("NW").setAzimuthCompass("SE")
                .riseAzimuthDegrees(315.0).setAzimuthDegrees(135.0)
                .maxElevationDegrees(45.0)
                .duration(Duration.ofMinutes(4))
                .magnitude(-1.0)
                .freshness(DataFreshness.LIVE);
    }

    @Given("a pass rising in {int} minutes towards the {string} direction")
    public void passRisingIn(int minutes, String direction) {
        passBuilder.riseTime(now.plus(Duration.ofMinutes(minutes))).riseAzimuthCompass(direction);
    }

    @Given("a pass lasting {int} minutes")
    public void passLasting(int minutes) {
        passBuilder.duration(Duration.ofMinutes(minutes));
    }

    @Given("the pass lasts {int} minutes")
    public void thePassLasts(int minutes) {
        passBuilder.duration(Duration.ofMinutes(minutes));
    }

    @Given("a pass with magnitude {double}")
    public void passWithMagnitude(double magnitude) {
        passBuilder.magnitude(magnitude);
    }

    @Given("the pass has magnitude {double}")
    public void thePassHasMagnitude(double magnitude) {
        passBuilder.magnitude(magnitude);
    }

    @Given("the cloud cover forecast is {int} percent")
    public void cloudCoverForecast(int percent) {
        forecast = CloudForecast.builder()
                .forecastHour(now.plus(Duration.ofMinutes(60)))
                .cloudCoverPercent(percent)
                .freshness(DataFreshness.LIVE)
                .build();
    }

    @When("the pass is evaluated")
    public void evaluate() {
        alerts = engine.evaluate(passBuilder.build(), forecast, now);
    }

    @Then("the alerts should include {string}")
    public void shouldInclude(String expected) {
        assertThat(alerts).anyMatch(a -> a.contains(expected));
    }

    @Then("the alerts should not include {string}")
    public void shouldNotInclude(String expected) {
        assertThat(alerts).noneMatch(a -> a.contains(expected));
    }

    @Then("exactly {int} alerts should be generated")
    public void exactlyNAlerts(int n) {
        assertThat(alerts).hasSize(n);
    }

    @Then("no alerts should be generated")
    public void noAlerts() {
        assertThat(alerts).isEmpty();
    }
}