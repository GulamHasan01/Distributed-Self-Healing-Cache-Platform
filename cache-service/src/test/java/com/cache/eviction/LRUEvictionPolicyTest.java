package com.cache.eviction;

import com.cache.model.CacheEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for LRUEvictionPolicy.
 *
 * <p>These tests exercise the pure algorithm without any Spring context.
 * We manually control lastAccessed by calling recordAccess() at controlled
 * intervals to simulate different access times.</p>
 */
@DisplayName("LRUEvictionPolicy Unit Tests")
class LRUEvictionPolicyTest {

    private LRUEvictionPolicy policy;

    @BeforeEach
    void setUp() {
        policy = new LRUEvictionPolicy();
    }

    @Test
    @DisplayName("should select the least recently accessed entry as victim")
    void shouldSelectLeastRecentlyUsed() throws InterruptedException {
        // Given: three entries with deliberately different access times
        CacheEntry oldest = new CacheEntry("old-key", "v1");
        Thread.sleep(5);

        CacheEntry middle = new CacheEntry("mid-key", "v2");
        middle.recordAccess(); // bump middle's lastAccessed
        Thread.sleep(5);

        CacheEntry newest = new CacheEntry("new-key", "v3");
        newest.recordAccess(); // bump newest's lastAccessed

        // When
        Optional<String> victim = policy.selectVictim(List.of(oldest, middle, newest));

        // Then: "old-key" was accessed least recently
        assertThat(victim).isPresent().contains("old-key");
    }

    @Test
    @DisplayName("should return empty Optional for empty collection")
    void shouldReturnEmptyForEmptyCollection() {
        Optional<String> victim = policy.selectVictim(List.of());
        assertThat(victim).isEmpty();
    }

    @Test
    @DisplayName("should return empty Optional for null collection")
    void shouldReturnEmptyForNullCollection() {
        Optional<String> victim = policy.selectVictim(null);
        assertThat(victim).isEmpty();
    }

    @Test
    @DisplayName("should return the only entry when collection has one element")
    void shouldReturnOnlyEntryForSingleElement() {
        CacheEntry single = new CacheEntry("only-key", "value");
        Optional<String> victim = policy.selectVictim(List.of(single));
        assertThat(victim).isPresent().contains("only-key");
    }

    @Test
    @DisplayName("should report correct policy name")
    void shouldReportPolicyName() {
        assertThat(policy.policyName()).isEqualTo("LRU");
    }
}
