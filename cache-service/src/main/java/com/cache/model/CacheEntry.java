package com.cache.model;

import java.time.Instant;

/**
 * Internal domain model representing a single cache entry with TTL and access metadata.
 * Thread-safe for read-write operations on metadata via synchronized access/volatile fields.
 */
public class CacheEntry {

    private final String key;
    private final String value;
    private final Instant createdAt;
    private volatile Instant lastAccessed;
    private volatile long accessCount;

    /**
     * TTL in seconds. -1 means no expiration.
     * This is the EFFECTIVE TTL for this entry — may come from the request
     * or from the cache-wide default TTL.
     */
    private final long ttlSeconds;



    /**
     * Create an entry with NO TTL (lives forever until evicted or deleted).
     * Used when neither the request nor the cache default specifies a TTL.
     */
    public CacheEntry(String key, String value) {
        this(key, value, -1L);
    }

    /**
     * Create an entry with an explicit TTL.
     *
     * @param key        the cache key
     * @param value      the value to store
     * @param ttlSeconds seconds until expiry; -1 means no expiry
     */
    public CacheEntry(String key, String value, long ttlSeconds) {
        this.key = key;
        this.value = value;
        this.createdAt = Instant.now();
        this.lastAccessed = Instant.now();
        this.accessCount = 0;
        this.ttlSeconds = ttlSeconds;
    }



    /**
     * Checks if the absolute time-to-live (TTL) of this entry has elapsed.
     */
    public boolean isExpired() {
        if (ttlSeconds <= 0) {
            return false; // no TTL configured → never expires
        }
        return Instant.now().isAfter(createdAt.plusSeconds(ttlSeconds));
    }

    /**
     * Returns the number of seconds remaining before expiry.
     * Returns -1 if no TTL is configured.
     * Returns 0 if already expired.
     */
    public long getRemainingTtlSeconds() {
        if (ttlSeconds <= 0) return -1;
        long elapsed = Instant.now().getEpochSecond() - createdAt.getEpochSecond();
        long remaining = ttlSeconds - elapsed;
        return Math.max(0, remaining);
    }

    /**
     * Called on every successful GET to update LRU access metadata.
     * The volatile write ensures all threads see the updated lastAccessed immediately.
     */
    public synchronized void recordAccess() {
        this.lastAccessed = Instant.now();
        this.accessCount++;
    }



    public String getKey() { return key; }
    public String getValue() { return value; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getLastAccessed() { return lastAccessed; }
    public long getAccessCount() { return accessCount; }
    public long getTtlSeconds() { return ttlSeconds; }
    public boolean hasTtl() { return ttlSeconds > 0; }

    @Override
    public String toString() {
        return "CacheEntry{key='" + key + "', ttl=" + ttlSeconds + "s, expired=" + isExpired() + "}";
    }
}
