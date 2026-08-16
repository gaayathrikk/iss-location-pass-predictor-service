package com.isspredictor.iss_predictor_service.model;

/**
 * Tags the provenance of any data returned to the client, so the frontend
 * (and Postman users) can always tell whether they're looking at a live
 * value, a stale cached value served during an upstream outage, or a
 * mock fallback used when even the cache is empty/expired.
 * <p>
 * This is the backbone of the Option A resilience strategy: every response
 * is honest about its own freshness rather than silently degrading.
 */
public enum DataFreshness {
    LIVE,
    CACHED_STALE,
    MOCK
}