/**
 * Outbound adapters to Open-Notify, Pollux Labs and Open-Meteo. Each upstream is wrapped behind an interface (Strategy pattern) so it is independently testable/mockable.
 */
package com.isspredictor.iss_predictor_service.client;

import com.isspredictor.iss_predictor_service.model.IssPosition;

/**
 * Abstraction over "wherever we get the ISS's current position from."
 * The service layer depends only on this interface, never on
 * {@code OpenNotifyClient} directly - this is what lets the client be
 * swapped or mocked without touching business logic (Dependency Inversion).
 */
public interface IssPositionClient {

    /**
     * Never throws for upstream failures - always returns a position,
     * tagged with the appropriate {@link com.isspredictor.model.DataFreshness}
     * (LIVE, CACHED_STALE, or MOCK) per the Option A degradation strategy.
     */
    IssPosition getCurrentPosition();
}