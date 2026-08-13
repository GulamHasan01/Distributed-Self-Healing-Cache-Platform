package com.cache.service;

import com.cache.config.CacheProperties;
import com.cache.dto.request.CachePutRequest;
import com.cache.dto.response.CacheEntryResponse;
import com.cache.dto.response.CacheStatsResponse;
import com.cache.exception.CacheKeyNotFoundException;
import com.cache.eviction.EvictionPolicy;
import com.cache.service.impl.CacheServiceImpl;
import com.cache.store.CacheStore;
import com.cache.model.CacheEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CacheServiceImpl.
 *
 * <p>WHY unit tests here (not integration tests)?</p>
 * We want to test the SERVICE logic in isolation.
 * We mock the CacheStore — we don't care HOW the store works here,
 * only that the service correctly calls the store and handles the results.
 * This is the purpose of the service interface: mockability.
 *
 * <p>Test structure follows the Given-When-Then (Arrange-Act-Assert) pattern.</p>
 *
 * <p>WHY @ExtendWith(MockitoExtension.class) not @SpringBootTest?</p>
 * @SpringBootTest loads the full Spring context — slow (seconds).
 * MockitoExtension creates mocks only — fast (milliseconds).
 * Unit tests should be fast. Integration tests use @SpringBootTest.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CacheService Unit Tests")
class CacheServiceImplTest {

    @Mock
    private CacheStore cacheStore;

    @Mock
    private CacheProperties cacheProperties;

    @Mock
    private CacheProperties.TtlProperties ttlProperties;

    @Mock
    private CacheProperties.NodeProperties nodeProperties;

    @Mock
    private EvictionPolicy evictionPolicy;

    @Mock
    private com.cache.cluster.replication.ReplicationService replicationService;

    private CacheServiceImpl cacheService;

    @BeforeEach
    void setUp() {
        lenient().when(cacheProperties.getMaxSize()).thenReturn(1000);
        lenient().when(cacheProperties.getNode()).thenReturn(nodeProperties);
        lenient().when(nodeProperties.getId()).thenReturn("node-1");
        lenient().when(cacheProperties.getTtl()).thenReturn(ttlProperties);
        lenient().when(ttlProperties.getDefaultSeconds()).thenReturn(-1L);
        lenient().when(replicationService.getStats()).thenReturn(com.cache.cluster.replication.ReplicationStats.empty());
        lenient().when(cacheStore.getEvictionPolicy()).thenReturn(evictionPolicy);
        lenient().when(evictionPolicy.policyName()).thenReturn("LRU");
        cacheService = new CacheServiceImpl(cacheStore, cacheProperties, evictionPolicy, replicationService);
    }

    // =========================================================================
    @Nested
    @DisplayName("PUT operation")
    class PutTests {

        @Test
        @DisplayName("should store entry and return response DTO")
        void shouldStoreEntrySuccessfully() {
            // Given
            CachePutRequest request = new CachePutRequest("user:1001", "{\"name\":\"Alice\"}", null);
            doNothing().when(cacheStore).put(any(CacheEntry.class));

            // When
            CacheEntryResponse response = cacheService.put(request);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.key()).isEqualTo("user:1001");
            assertThat(response.value()).isEqualTo("{\"name\":\"Alice\"}");
            assertThat(response.createdAt()).isNotNull();
            verify(cacheStore, times(1)).put(any(CacheEntry.class));
        }

        @Test
        @DisplayName("should propagate exception when store throws")
        void shouldPropagateStoreException() {
            // Given
            CachePutRequest request = new CachePutRequest("key", "value", null);
            doThrow(new RuntimeException("store failure")).when(cacheStore).put(any());

            // When / Then
            assertThatThrownBy(() -> cacheService.put(request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("store failure");
        }
    }

    // =========================================================================
    @Nested
    @DisplayName("GET operation")
    class GetTests {

        @Test
        @DisplayName("should return entry when key exists")
        void shouldReturnEntryOnHit() {
            // Given
            CacheEntry entry = new CacheEntry("user:1001", "{\"name\":\"Alice\"}");
            when(cacheStore.get("user:1001")).thenReturn(Optional.of(entry));

            // When
            CacheEntryResponse response = cacheService.get("user:1001");

            // Then
            assertThat(response.key()).isEqualTo("user:1001");
            assertThat(response.value()).isEqualTo("{\"name\":\"Alice\"}");
            verify(cacheStore).get("user:1001");
        }

        @Test
        @DisplayName("should throw CacheKeyNotFoundException on cache miss")
        void shouldThrowOnCacheMiss() {
            // Given
            when(cacheStore.get("missing-key")).thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> cacheService.get("missing-key"))
                    .isInstanceOf(CacheKeyNotFoundException.class)
                    .hasMessageContaining("missing-key");
        }

        @Test
        @DisplayName("should increment hit counter on cache hit")
        void shouldIncrementHitCounter() {
            // Given
            CacheEntry entry = new CacheEntry("key", "value");
            when(cacheStore.get("key")).thenReturn(Optional.of(entry));
            when(cacheStore.size()).thenReturn(1L);

            // When
            cacheService.get("key");
            CacheStatsResponse stats = cacheService.getStats();

            // Then
            assertThat(stats.totalHits()).isEqualTo(1L);
            assertThat(stats.totalMisses()).isEqualTo(0L);
        }

        @Test
        @DisplayName("should increment miss counter on cache miss")
        void shouldIncrementMissCounter() {
            // Given
            when(cacheStore.get("missing")).thenReturn(Optional.empty());
            when(cacheStore.size()).thenReturn(0L);

            // When
            assertThatThrownBy(() -> cacheService.get("missing"))
                    .isInstanceOf(CacheKeyNotFoundException.class);
            CacheStatsResponse stats = cacheService.getStats();

            // Then
            assertThat(stats.totalHits()).isEqualTo(0L);
            assertThat(stats.totalMisses()).isEqualTo(1L);
        }
    }

    // =========================================================================
    @Nested
    @DisplayName("DELETE operation")
    class DeleteTests {

        @Test
        @DisplayName("should delete existing key without throwing")
        void shouldDeleteExistingKey() {
            // Given
            when(cacheStore.delete("user:1001")).thenReturn(true);

            // When / Then
            assertThatCode(() -> cacheService.delete("user:1001"))
                    .doesNotThrowAnyException();
            verify(cacheStore).delete("user:1001");
        }

        @Test
        @DisplayName("should throw CacheKeyNotFoundException for non-existent key")
        void shouldThrowForNonExistentKey() {
            // Given
            when(cacheStore.delete("ghost-key")).thenReturn(false);

            // When / Then
            assertThatThrownBy(() -> cacheService.delete("ghost-key"))
                    .isInstanceOf(CacheKeyNotFoundException.class)
                    .hasMessageContaining("ghost-key");
        }
    }

    // =========================================================================
    @Nested
    @DisplayName("CLEAR operation")
    class ClearTests {

        @Test
        @DisplayName("should clear all entries and return count")
        void shouldClearAndReturnCount() {
            // Given
            when(cacheStore.clear()).thenReturn(42L);

            // When
            long removed = cacheService.clear();

            // Then
            assertThat(removed).isEqualTo(42L);
            verify(cacheStore).clear();
        }
    }

    // =========================================================================
    @Nested
    @DisplayName("STATS operation")
    class StatsTests {

        @Test
        @DisplayName("should return correct stats with zero hits and misses")
        void shouldReturnCorrectStatsWhenEmpty() {
            // Given
            when(cacheStore.size()).thenReturn(0L);

            // When
            CacheStatsResponse stats = cacheService.getStats();

            // Then
            assertThat(stats.nodeId()).isEqualTo("node-1");
            assertThat(stats.totalKeys()).isEqualTo(0L);
            assertThat(stats.maxCapacity()).isEqualTo(1000);
            assertThat(stats.usagePercent()).isEqualTo(0.0);
            assertThat(stats.totalHits()).isEqualTo(0L);
            assertThat(stats.totalMisses()).isEqualTo(0L);
            assertThat(stats.hitRatio()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("should calculate usage percent correctly")
        void shouldCalculateUsagePercent() {
            // Given: 500 keys in a 1000 max store = 50% usage
            when(cacheStore.size()).thenReturn(500L);

            // When
            CacheStatsResponse stats = cacheService.getStats();

            // Then
            assertThat(stats.usagePercent()).isEqualTo(50.0);
        }
    }

    // =========================================================================
    @Nested
    @DisplayName("updateEvictionPolicy operation")
    class UpdateEvictionPolicyTests {

        @Test
        @DisplayName("should update eviction policy to FIFO successfully")
        void shouldUpdateEvictionPolicy() {
            // When
            cacheService.updateEvictionPolicy("FIFO");

            // Then
            verify(cacheStore).setEvictionPolicy(any(com.cache.eviction.FIFOEvictionPolicy.class));
        }

        @Test
        @DisplayName("should throw IllegalArgumentException for unknown policy")
        void shouldThrowForUnknownPolicy() {
            assertThatThrownBy(() -> cacheService.updateEvictionPolicy("UNKNOWN_XYZ"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Unknown eviction policy");
        }
    }
}
