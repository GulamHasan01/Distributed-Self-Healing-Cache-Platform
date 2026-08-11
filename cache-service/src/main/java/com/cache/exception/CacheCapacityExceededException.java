package com.cache.exception;

/**
 * Thrown when a PUT is attempted but the cache store has reached its maximum
 * configured capacity and no eviction policy is in place (Phase 1).
 *
 * <p>Phase 2 will introduce LRU eviction, at which point this exception
 * will be thrown only in extreme edge cases or removed entirely.</p>
 */
public class CacheCapacityExceededException extends RuntimeException {

    private final int maxCapacity;
    private final long currentSize;

    public CacheCapacityExceededException(int maxCapacity, long currentSize) {
        super(String.format(
                "Cache capacity exceeded. Max: %d, Current: %d. " +
                "No eviction policy is configured — upgrade to Phase 2 for LRU support.",
                maxCapacity, currentSize
        ));
        this.maxCapacity = maxCapacity;
        this.currentSize = currentSize;
    }

    public CacheCapacityExceededException(String message) {
        super(message);
        this.maxCapacity = 0;
        this.currentSize = 0;
    }

    public int getMaxCapacity() { return maxCapacity; }
    public long getCurrentSize() { return currentSize; }
}
