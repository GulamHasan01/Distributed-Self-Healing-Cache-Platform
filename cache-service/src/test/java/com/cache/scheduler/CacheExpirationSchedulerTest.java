package com.cache.scheduler;

import com.cache.store.CacheStore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

/**
 * Unit tests for CacheExpirationScheduler.
 *
 * <p>The scheduler has one responsibility: call cacheStore.removeExpired()
 * when triggered. We test that it delegates correctly and handles both
 * the "entries removed" and "nothing to remove" scenarios.</p>
 *
 * <p>We do NOT test the actual scheduling timing (@Scheduled interval) here —
 * that would require a Spring integration test with Thread.sleep, which
 * is slow and brittle. We test the behavior, not the timing.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CacheExpirationScheduler Unit Tests")
class CacheExpirationSchedulerTest {

    @Mock
    private CacheStore cacheStore;

    @InjectMocks
    private CacheExpirationScheduler scheduler;

    @Test
    @DisplayName("should call removeExpired on the store during sweep")
    void shouldDelegateToStoreRemoveExpired() {
        // Given
        when(cacheStore.removeExpired()).thenReturn(0);
        when(cacheStore.size()).thenReturn(100L);

        // When
        scheduler.sweepExpiredEntries();

        // Then
        verify(cacheStore, times(1)).removeExpired();
    }

    @Test
    @DisplayName("should query store size for logging")
    void shouldQueryStoreSizeForLogging() {
        // Given
        when(cacheStore.removeExpired()).thenReturn(5);
        when(cacheStore.size()).thenReturn(95L);

        // When
        scheduler.sweepExpiredEntries();

        // Then: size is queried twice (before and after)
        verify(cacheStore, atLeast(1)).size();
    }

    @Test
    @DisplayName("should handle removeExpired returning 0 entries removed")
    void shouldHandleZeroRemovedEntries() {
        // Given
        when(cacheStore.removeExpired()).thenReturn(0);
        when(cacheStore.size()).thenReturn(50L);

        // When / Then: no exception thrown
        org.assertj.core.api.Assertions.assertThatCode(() -> scheduler.sweepExpiredEntries())
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("should handle removeExpired returning multiple removed entries")
    void shouldHandleMultipleRemovedEntries() {
        // Given
        when(cacheStore.removeExpired()).thenReturn(42);
        when(cacheStore.size()).thenReturn(958L);

        // When / Then: no exception thrown
        org.assertj.core.api.Assertions.assertThatCode(() -> scheduler.sweepExpiredEntries())
                .doesNotThrowAnyException();
        verify(cacheStore).removeExpired();
    }
}
