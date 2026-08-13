package com.cache.store;

import com.cache.config.CacheProperties;
import com.cache.eviction.LRUEvictionPolicy;
import com.cache.eviction.NoEvictionPolicy;
import com.cache.exception.CacheCapacityExceededException;
import com.cache.model.CacheEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for InMemoryCacheStore — Phase 2 edition.
 *
 * <p>Tests cover:</p>
 * <ul>
 *   <li>LRU eviction when the store is full</li>
 *   <li>NoEviction policy rejection</li>
 *   <li>Lazy TTL expiry on GET</li>
 *   <li>Eager TTL expiry via removeExpired()</li>
 *   <li>Eviction and expiry counters</li>
 * </ul>
 */
import com.cache.persistence.PersistenceService;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for InMemoryCacheStore — Phase 2 & Phase 10 edition.
 */
@DisplayName("InMemoryCacheStore Phase 2 Tests")
class InMemoryCacheStoreTest {

    private CacheProperties properties;
    private PersistenceService persistenceService = mock(PersistenceService.class);

    private io.micrometer.core.instrument.simple.SimpleMeterRegistry meterRegistry = new io.micrometer.core.instrument.simple.SimpleMeterRegistry();

    @BeforeEach
    void setUp() {
        properties = new CacheProperties();
        properties.setMaxSize(3); // Small limit to trigger eviction easily
    }

    private InMemoryCacheStore storeWithLRU() {
        return new InMemoryCacheStore(properties, new LRUEvictionPolicy(), persistenceService, meterRegistry);
    }

    private InMemoryCacheStore storeWithNoEviction() {
        return new InMemoryCacheStore(properties, new NoEvictionPolicy(), persistenceService, meterRegistry);
    }

    // =========================================================================
    @Nested
    @DisplayName("Basic PUT / GET / DELETE")
    class BasicOpsTests {

        @Test
        @DisplayName("should store and retrieve an entry without TTL")
        void shouldStoreAndRetrieve() {
            var store = storeWithLRU();
            store.put(new CacheEntry("k1", "v1"));

            Optional<CacheEntry> result = store.get("k1");

            assertThat(result).isPresent();
            assertThat(result.get().getValue()).isEqualTo("v1");
        }

        @Test
        @DisplayName("should return empty for non-existent key")
        void shouldReturnEmptyForMiss() {
            var store = storeWithLRU();
            assertThat(store.get("ghost")).isEmpty();
        }

        @Test
        @DisplayName("should overwrite existing key")
        void shouldOverwriteKey() {
            var store = storeWithLRU();
            store.put(new CacheEntry("k1", "original"));
            store.put(new CacheEntry("k1", "updated"));

            assertThat(store.size()).isEqualTo(1);
            assertThat(store.get("k1").get().getValue()).isEqualTo("updated");
        }

        @Test
        @DisplayName("should delete existing key and return true")
        void shouldDeleteKey() {
            var store = storeWithLRU();
            store.put(new CacheEntry("k1", "v1"));

            assertThat(store.delete("k1")).isTrue();
            assertThat(store.containsKey("k1")).isFalse();
        }

        @Test
        @DisplayName("should return false when deleting non-existent key")
        void shouldReturnFalseOnMissDelete() {
            var store = storeWithLRU();
            assertThat(store.delete("ghost")).isFalse();
        }

        @Test
        @DisplayName("should clear all entries and return count")
        void shouldClearAll() {
            var store = storeWithLRU();
            store.put(new CacheEntry("k1", "v1"));
            store.put(new CacheEntry("k2", "v2"));

            assertThat(store.clear()).isEqualTo(2);
            assertThat(store.isEmpty()).isTrue();
        }
    }

    // =========================================================================
    @Nested
    @DisplayName("LRU Eviction")
    class LruEvictionTests {

        @Test
        @DisplayName("should evict the least recently used entry when store is full")
        void shouldEvictLRUEntry() throws InterruptedException {
            var store = storeWithLRU();

            // Fill the store (maxSize=3)
            store.put(new CacheEntry("k1", "v1")); // will be LRU
            Thread.sleep(5);
            store.put(new CacheEntry("k2", "v2"));
            Thread.sleep(5);
            store.put(new CacheEntry("k3", "v3"));

            // Access k1 and k2 to make k3... wait, k1 was created first
            // and never accessed again, so it should be LRU
            // Access k2 to push its lastAccessed forward
            store.get("k2");

            // Now store is full: k1(oldest), k3(created last), k2(just accessed)
            // LRU victim should be k1 (oldest lastAccessed)

            // When: insert k4 (store is full → eviction needed)
            store.put(new CacheEntry("k4", "v4"));

            // Then: k1 should have been evicted
            assertThat(store.containsKey("k1")).isFalse();
            assertThat(store.containsKey("k2")).isTrue();
            assertThat(store.containsKey("k3")).isTrue();
            assertThat(store.containsKey("k4")).isTrue();
            assertThat(store.size()).isEqualTo(3);
        }

        @Test
        @DisplayName("should increment eviction counter on each eviction")
        void shouldIncrementEvictionCounter() throws InterruptedException {
            var store = storeWithLRU();

            store.put(new CacheEntry("k1", "v1"));
            Thread.sleep(2);
            store.put(new CacheEntry("k2", "v2"));
            Thread.sleep(2);
            store.put(new CacheEntry("k3", "v3"));

            // Store is full: two more inserts = two evictions
            store.put(new CacheEntry("k4", "v4"));
            store.put(new CacheEntry("k5", "v5"));

            assertThat(store.getEvictionCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("should allow overwrite when at max capacity without eviction")
        void shouldAllowOverwriteAtMaxCapacity() {
            var store = storeWithLRU();

            store.put(new CacheEntry("k1", "v1"));
            store.put(new CacheEntry("k2", "v2"));
            store.put(new CacheEntry("k3", "v3"));
            long evictionsBefore = store.getEvictionCount();

            // Overwrite k1 — should NOT trigger eviction
            assertThatCode(() -> store.put(new CacheEntry("k1", "updated")))
                    .doesNotThrowAnyException();
            assertThat(store.getEvictionCount()).isEqualTo(evictionsBefore);
        }
    }

    // =========================================================================
    @Nested
    @DisplayName("NoEviction Policy")
    class NoEvictionPolicyTests {

        @Test
        @DisplayName("should throw CacheCapacityExceededException when full with NoEviction policy")
        void shouldThrowWhenFullAndNoEviction() {
            var store = storeWithNoEviction();

            store.put(new CacheEntry("k1", "v1"));
            store.put(new CacheEntry("k2", "v2"));
            store.put(new CacheEntry("k3", "v3"));

            assertThatThrownBy(() -> store.put(new CacheEntry("k4", "v4")))
                    .isInstanceOf(CacheCapacityExceededException.class)
                    .hasMessageContaining("Cache capacity exceeded");
        }

        @Test
        @DisplayName("should NOT increment eviction counter with NoEviction policy")
        void shouldNotIncrementEvictionCounter() {
            var store = storeWithNoEviction();

            store.put(new CacheEntry("k1", "v1"));
            store.put(new CacheEntry("k2", "v2"));
            store.put(new CacheEntry("k3", "v3"));

            // This will throw, but eviction count should remain 0
            assertThatThrownBy(() -> store.put(new CacheEntry("k4", "v4")))
                    .isInstanceOf(CacheCapacityExceededException.class);

            assertThat(store.getEvictionCount()).isEqualTo(0);
        }
    }

    // =========================================================================
    @Nested
    @DisplayName("TTL Expiry — Lazy (on GET)")
    class LazyTtlExpiryTests {

        @Test
        @DisplayName("should return entry when TTL has not yet elapsed")
        void shouldReturnEntryBeforeTtlExpiry() {
            var store = storeWithLRU();
            // TTL of 60 seconds — will not expire in this test
            store.put(new CacheEntry("k1", "v1", 60L));

            assertThat(store.get("k1")).isPresent();
        }

        @Test
        @DisplayName("should return empty and remove entry when TTL has elapsed (lazy expiry)")
        void shouldReturnEmptyAfterTtlExpiry() throws InterruptedException {
            var store = storeWithLRU();
            // 1-second TTL
            store.put(new CacheEntry("k1", "v1", 1L));

            Thread.sleep(1100); // wait for TTL to elapse

            // Lazy expiry: the GET should detect expiry and remove
            assertThat(store.get("k1")).isEmpty();
            assertThat(store.containsKey("k1")).isFalse();
        }

        @Test
        @DisplayName("should increment expiredCount on lazy expiry")
        void shouldIncrementExpiredCountOnLazyExpiry() throws InterruptedException {
            var store = storeWithLRU();
            store.put(new CacheEntry("k1", "v1", 1L));

            Thread.sleep(1100);

            store.get("k1"); // triggers lazy expiry
            assertThat(store.getExpiredCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("entry with no TTL should never expire")
        void entryWithNoTtlShouldNotExpire() throws InterruptedException {
            var store = storeWithLRU();
            store.put(new CacheEntry("k1", "v1")); // no TTL (-1)

            Thread.sleep(100);

            assertThat(store.get("k1")).isPresent();
        }
    }

    // =========================================================================
    @Nested
    @DisplayName("TTL Expiry — Eager (removeExpired sweep)")
    class EagerTtlExpiryTests {

        @Test
        @DisplayName("should remove expired entries during sweep")
        void shouldRemoveExpiredEntries() throws InterruptedException {
            var store = storeWithLRU();
            store.put(new CacheEntry("expires", "v1", 1L));    // 1s TTL
            store.put(new CacheEntry("immortal", "v2"));        // no TTL

            Thread.sleep(1100);

            int removed = store.removeExpired();

            assertThat(removed).isEqualTo(1);
            assertThat(store.containsKey("expires")).isFalse();
            assertThat(store.containsKey("immortal")).isTrue();
        }

        @Test
        @DisplayName("should return 0 when no entries have expired")
        void shouldReturn0WhenNothingExpired() {
            var store = storeWithLRU();
            store.put(new CacheEntry("k1", "v1", 60L)); // 60s TTL — won't expire

            assertThat(store.removeExpired()).isEqualTo(0);
        }

        @Test
        @DisplayName("should increment expiredCount for each entry removed by sweep")
        void shouldIncrementExpiredCountOnSweep() throws InterruptedException {
            var store = storeWithLRU();
            store.put(new CacheEntry("k1", "v1", 1L));
            store.put(new CacheEntry("k2", "v2", 1L));

            Thread.sleep(1100);

            store.removeExpired();
            assertThat(store.getExpiredCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("should return 0 when store is empty")
        void shouldReturn0OnEmptyStore() {
            var store = storeWithLRU();
            assertThat(store.removeExpired()).isEqualTo(0);
        }
    }
}
