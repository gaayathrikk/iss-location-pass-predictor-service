/**
 * In-memory TTL cache + stale/degradation strategy (Option A).
 */
package com.isspredictor.iss_predictor_service.cache;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Generic in-memory cache with per-entry TTL and stale-serving support.
 * <p>
 * This is the core of the Option A resilience strategy. Three read modes:
 * <ul>
 *   <li>{@link #getFresh} - only returns a value if it's still within its TTL</li>
 *   <li>{@link #getStaleIfWithin} - returns a value even if expired, as long as
 *       it's not older than the given ceiling (e.g. 6 hours)</li>
 *   <li>{@link #put} - stores a value with its own TTL</li>
 * </ul>
 * No external cache library (Caffeine, Redis) is used here deliberately -
 * the problem statement requires no DB, and a single-instance deployment
 * doesn't need a distributed cache. A {@link ConcurrentHashMap} is thread-safe
 * enough for this workload.
 */
public class TtlCache<K, V> {

    private record CacheEntry<V>(V value, Instant cachedAt, Duration ttl) {
        boolean isFresh() {
            return Instant.now().isBefore(cachedAt.plus(ttl));
        }

        boolean isWithin(Duration maxAge) {
            return Instant.now().isBefore(cachedAt.plus(maxAge));
        }
    }

    private final Map<K, CacheEntry<V>> store = new ConcurrentHashMap<>();

    public void put(K key, V value, Duration ttl) {
        store.put(key, new CacheEntry<>(value, Instant.now(), ttl));
    }

    /** Returns the value only if it's still within its original TTL - empty otherwise. */
    public Optional<V> getFresh(K key) {
        CacheEntry<V> entry = store.get(key);
        if (entry == null || !entry.isFresh()) {
            return Optional.empty();
        }
        return Optional.of(entry.value());
    }

    /**
     * Returns the value even if it's past its TTL, as long as it's not older
     * than {@code maxAge}. This is what powers "serve stale data on upstream
     * failure" - the entry doesn't have to be fresh, just not ancient.
     */
    public Optional<V> getStaleIfWithin(K key, Duration maxAge) {
        CacheEntry<V> entry = store.get(key);
        if (entry == null || !entry.isWithin(maxAge)) {
            return Optional.empty();
        }
        return Optional.of(entry.value());
    }

    /** Removes any entry older than {@code maxAge}, regardless of key. Called by the scheduled sweep. */
    public void evictOlderThan(Duration maxAge) {
        store.entrySet().removeIf(e -> !e.getValue().isWithin(maxAge));
    }

    public int size() {
        return store.size();
    }
}