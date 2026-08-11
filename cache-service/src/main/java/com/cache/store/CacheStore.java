package com.cache.store;

import com.cache.model.CacheEntry;

import java.util.Collection;
import java.util.Optional;

/**
 * Storage abstraction for the cache.
 * Keeps track of active entries, handles basic operations, and provides metrics.
 */
public interface CacheStore {

    /**
     * Store a key-value pair. Overwrites existing entry if key already exists.
     * If the store is full and an eviction policy is configured, one entry is evicted
     * before insertion.
     *
     * @param entry the cache entry to store
     * @throws com.cache.exception.CacheCapacityExceededException if the store is full
     *         and the configured eviction policy cannot produce a victim
     */
    void put(CacheEntry entry);

    /**
     * Retrieve an entry by key.
     * Returns empty if the key does not exist OR if the entry is expired.
     * Expired entries are treated as misses and removed lazily on access.
     *
     * @param key the key to look up
     * @return Optional containing the live entry, or empty on miss/expiry
     */
    Optional<CacheEntry> get(String key);

    /**
     * Remove a specific key from the store.
     *
     * @param key the key to remove
     * @return true if the key existed and was removed, false if it did not exist
     */
    boolean delete(String key);

    /**
     * Remove ALL entries from the store.
     *
     * @return number of entries that were removed
     */
    long clear();

    /**
     * Scan the entire store and remove all expired entries.
     * Called by {@link com.cache.scheduler.CacheExpirationScheduler} on a fixed schedule.
     *
     * @return number of entries removed due to TTL expiry
     */
    int removeExpired();

    /**
     * @return current number of LIVE (non-expired) entries
     */
    long size();

    boolean isEmpty();

    boolean containsKey(String key);

    /**
     * Return all entries — used for stats, replication, and migration.
     *
     * @return unmodifiable snapshot of all entries (including expired ones not yet swept)
     */
    Collection<CacheEntry> getAllEntries();



    /** Total number of entries evicted due to capacity limits since startup. */
    long getEvictionCount();

    /** Total number of entries removed due to TTL expiry since startup. */
    long getExpiredCount();



    /** Set the active eviction policy strategy */
    void setEvictionPolicy(com.cache.eviction.EvictionPolicy policy);

    /** Get the active eviction policy strategy */
    com.cache.eviction.EvictionPolicy getEvictionPolicy();
}
