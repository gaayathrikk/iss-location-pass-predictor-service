/**
 * Stateless helpers (azimuth formatting, magnitude math) with no Spring dependencies -- kept pure for easy unit testing.
 */
package com.isspredictor.iss_predictor_service.util;

/**
 * Converts a compass bearing in degrees to an 8-point compass label.
 * Pure, stateless, no Spring dependency - deliberately kept this way so it's
 * a one-line unit test per direction and reusable anywhere (currently used by
 * both {@code PolluxLabsClient} and the alert engine's direction messaging).
 */
public final class AzimuthFormatter {

    private static final String[] COMPASS_POINTS = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};

    private AzimuthFormatter() {
        // utility class, no instances
    }

    public static String toCompass(double degrees) {
        double normalized = ((degrees % 360) + 360) % 360; // handles negative input safely
        int index = (int) Math.round(normalized / 45.0) % 8;
        return COMPASS_POINTS[index];
    }
}