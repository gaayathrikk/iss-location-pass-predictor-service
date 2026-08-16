package com.isspredictor.iss_predictor_service.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

/**
 * Periodically sweeps all registered {@link TtlCache} instances, evicting any
 * entry older than the configured hard ceiling
 * ({@code iss.cache.max-stale-serve-seconds}, default 6 hours).
 * <p>
 * Deliberately does NOT evict merely-expired-but-still-stale-serveable entries -
 * only entries past the absolute ceiling are removed. This is what lets
 * {@link TtlCache#getStaleIfWithin} keep serving degraded data for hours during
 * a prolonged upstream outage, while still bounding memory growth and making
 * sure nothing ancient is ever served as "stale".
 */
@Component
@Slf4j
public class CacheEvictionScheduler {

    private final List<TtlCache<?, ?>> caches;
    private final Duration maxStaleServe;

    public CacheEvictionScheduler(
            List<TtlCache<?, ?>> caches,
            @Value("${iss.cache.max-stale-serve-seconds}") long maxStaleServeSeconds) {
        this.caches = caches;
        this.maxStaleServe = Duration.ofSeconds(maxStaleServeSeconds);
    }

    /** Runs every 60s - frequent enough to bound memory, cheap enough not to matter. */
    @Scheduled(fixedDelay = 60_000)
    public void evictExpiredEntries() {
        caches.forEach(cache -> cache.evictOlderThan(maxStaleServe));
        log.debug("Cache eviction sweep completed across {} cache(s)", caches.size());
    }
}