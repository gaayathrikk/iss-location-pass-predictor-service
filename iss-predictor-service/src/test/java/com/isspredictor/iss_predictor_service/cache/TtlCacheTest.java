package com.isspredictor.iss_predictor_service.cache;

import org.junit.jupiter.api.Test;
import java.time.Duration;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Uses short real TTLs + Thread.sleep rather than a mocked clock - TtlCache
 * has no Clock dependency injected (deliberately, to keep it simple), so this
 * is the straightforward way to test its time-based behavior. Sleep durations
 * are chosen with generous margins (10x+ the TTL being tested) to keep this
 * reliable on a loaded CI runner without being flaky.
 */
class TtlCacheTest {

    @Test
    void getFreshReturnsValueBeforeTtlExpires() {
        TtlCache<String, String> cache = new TtlCache<>();
        cache.put("key", "value", Duration.ofSeconds(10));

        assertThat(cache.getFresh("key")).contains("value");
    }

    @Test
    void getFreshReturnsEmptyForMissingKey() {
        TtlCache<String, String> cache = new TtlCache<>();

        assertThat(cache.getFresh("nonexistent")).isEmpty();
    }

    @Test
    void getFreshReturnsEmptyAfterTtlExpires() throws InterruptedException {
        TtlCache<String, String> cache = new TtlCache<>();
        cache.put("key", "value", Duration.ofMillis(20));

        Thread.sleep(200); // well past the 20ms TTL

        assertThat(cache.getFresh("key")).isEmpty();
    }

    @Test
    void getStaleIfWithinReturnsValueEvenAfterTtlExpires() throws InterruptedException {
        TtlCache<String, String> cache = new TtlCache<>();
        cache.put("key", "value", Duration.ofMillis(20));

        Thread.sleep(200); // expired for getFresh purposes...

        // ...but still within a generous stale-serving ceiling
        assertThat(cache.getStaleIfWithin("key", Duration.ofSeconds(10))).contains("value");
    }

    @Test
    void getStaleIfWithinReturnsEmptyPastTheCeiling() throws InterruptedException {
        TtlCache<String, String> cache = new TtlCache<>();
        cache.put("key", "value", Duration.ofMillis(20));

        Thread.sleep(200);

        // ceiling itself has now also passed
        assertThat(cache.getStaleIfWithin("key", Duration.ofMillis(50))).isEmpty();
    }

    @Test
    void getStaleIfWithinReturnsEmptyForMissingKey() {
        TtlCache<String, String> cache = new TtlCache<>();

        assertThat(cache.getStaleIfWithin("nonexistent", Duration.ofHours(6))).isEmpty();
    }

    @Test
    void putOverwritesExistingEntryAndResetsItsAge() throws InterruptedException {
        TtlCache<String, String> cache = new TtlCache<>();
        cache.put("key", "old-value", Duration.ofMillis(20));

        Thread.sleep(50); // old entry now expired

        cache.put("key", "new-value", Duration.ofSeconds(10)); // fresh write

        assertThat(cache.getFresh("key")).contains("new-value");
    }

    @Test
    void evictOlderThanRemovesEntriesPastTheGivenAge() throws InterruptedException {
        TtlCache<String, String> cache = new TtlCache<>();
        cache.put("old", "value", Duration.ofMillis(20));
        Thread.sleep(200);
        cache.put("recent", "value", Duration.ofSeconds(10)); // written just now

        cache.evictOlderThan(Duration.ofMillis(50));

        assertThat(cache.getStaleIfWithin("old", Duration.ofHours(1))).isEmpty();     // evicted
        assertThat(cache.getStaleIfWithin("recent", Duration.ofHours(1))).contains("value"); // kept
    }

    @Test
    void evictOlderThanDoesNotRemoveEntriesStillWithinTheCeiling() throws InterruptedException {
        TtlCache<String, String> cache = new TtlCache<>();
        cache.put("key", "value", Duration.ofMillis(20)); // TTL expired, but...

        Thread.sleep(50);
        cache.evictOlderThan(Duration.ofSeconds(10)); // ...well within this eviction ceiling

        assertThat(cache.getStaleIfWithin("key", Duration.ofSeconds(10))).contains("value");
    }

    @Test
    void sizeReflectsNumberOfEntries() {
        TtlCache<String, String> cache = new TtlCache<>();
        cache.put("a", "1", Duration.ofSeconds(10));
        cache.put("b", "2", Duration.ofSeconds(10));

        assertThat(cache.size()).isEqualTo(2);
    }
}