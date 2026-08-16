Feature: ISS Visibility Alerts
  As a stargazer
  I want to receive alerts about upcoming ISS passes
  So that I know when and how to view the ISS

  Background:
    Given the visibility alert engine is configured with a 120 minute visible-soon window, 20% favourable cloud cover threshold, 5 minute extended duration threshold, and -2.0 bright magnitude threshold

  Scenario: ISS visible soon triggers a check-the-sky alert
    Given a pass rising in 60 minutes towards the "NW" direction
    And the cloud cover forecast is 50 percent
    When the pass is evaluated
    Then the alerts should include "ISS visible soon - Check sky in NW"

  Scenario: Pass far in the future does not trigger the visible-soon alert
    Given a pass rising in 180 minutes towards the "NW" direction
    And the cloud cover forecast is 50 percent
    When the pass is evaluated
    Then the alerts should not include "ISS visible soon - Check sky in NW"

  Scenario: Clear skies and an imminent pass together trigger the favourable-viewing alert
    Given a pass rising in 60 minutes towards the "NW" direction
    And the cloud cover forecast is 10 percent
    When the pass is evaluated
    Then the alerts should include "Clear skies predicted - Excellent viewing opportunity"

  Scenario: Cloudy skies suppress the favourable-viewing alert even if the pass is imminent
    Given a pass rising in 60 minutes towards the "NW" direction
    And the cloud cover forecast is 80 percent
    When the pass is evaluated
    Then the alerts should not include "Clear skies predicted - Excellent viewing opportunity"

  Scenario Outline: Extended viewing alert depends on pass duration
    Given a pass lasting <duration> minutes
    And the cloud cover forecast is 50 percent
    When the pass is evaluated
    Then the alerts should <expectation> "Extended viewing opportunity"

    Examples:
      | duration | expectation |
      | 4        | not include |
      | 5        | not include |
      | 6        | include     |
      | 10       | include     |

  Scenario Outline: Exceptionally bright alert depends on magnitude
    Given a pass with magnitude <magnitude>
    And the cloud cover forecast is 50 percent
    When the pass is evaluated
    Then the alerts should <expectation> "Exceptionally bright pass - Easily visible"

    Examples:
      | magnitude | expectation |
      | -1.0      | not include |
      | -2.0      | not include |
      | -2.5      | include     |
      | -4.0      | include     |

  Scenario: An ideal pass triggers all four alerts simultaneously
    Given a pass rising in 60 minutes towards the "NW" direction
    And the pass lasts 7 minutes
    And the pass has magnitude -3.5
    And the cloud cover forecast is 10 percent
    When the pass is evaluated
    Then exactly 4 alerts should be generated

  Scenario: An unremarkable, distant pass triggers no alerts
    Given a pass rising in 600 minutes towards the "NW" direction
    And the pass lasts 2 minutes
    And the pass has magnitude 2.0
    And the cloud cover forecast is 90 percent
    When the pass is evaluated
    Then no alerts should be generated