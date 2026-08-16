/**
 * Core business logic: orchestration, visibility/alert engine, brightness calc. No 3rd-party libs used for these calculations.
 */
package com.isspredictor.iss_predictor_service.service;

import org.springframework.stereotype.Component;

/**
 * Estimates the ISS's apparent visual magnitude for a pass based on its
 * maximum elevation angle.
 * <p>
 * This is a simplified model, not a rigorous astronomical calculation (a real
 * one would also factor in solar phase angle, atmospheric extinction, and
 * exact range to the satellite). The core intuition it captures is accurate
 * though: a pass directly overhead (elevation near 90 degrees) is much closer
 * to the observer and reflects more sunlight toward them than a pass low on
 * the horizon, so higher elevation reliably means brighter (lower/more
 * negative magnitude).
 * <p>
 * No third-party astronomy library used here per the "no 3rd-party libs for
 * core logic" requirement - this is intentionally simple, transparent math.
 */
@Component
public class MagnitudeCalculator {

    /** Brightest plausible magnitude, reserved for a near-overhead pass. */
    private static final double BRIGHTEST_MAGNITUDE = -4.0;

    /** Dimmest magnitude assigned to a pass just above the horizon. */
    private static final double DIMMEST_MAGNITUDE = 0.5;

    /**
     * @param maxElevationDegrees highest elevation angle reached during the pass (0-90)
     * @return estimated visual magnitude; lower (more negative) is brighter
     */
    public double estimateMagnitude(double maxElevationDegrees) {
        double clamped = Math.max(0.0, Math.min(90.0, maxElevationDegrees));
        double elevationFraction = clamped / 90.0;

        // Linear interpolation between dimmest (at 0 deg) and brightest (at 90 deg).
        return DIMMEST_MAGNITUDE - (elevationFraction * (DIMMEST_MAGNITUDE - BRIGHTEST_MAGNITUDE));
    }
}