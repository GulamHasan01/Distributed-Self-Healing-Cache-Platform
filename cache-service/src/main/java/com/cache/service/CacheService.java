package com.cache.service;

import com.cache.dto.request.CachePutRequest;
import com.cache.dto.response.CacheEntryResponse;
import com.cache.dto.response.CacheStatsResponse;

/**
 * Service interface for cache operations.
 *
 * <p>WHY a service interface?</p>
 * 1. The controller depends on this interface, not the implementation.
 *    In tests, we can inject a mock of CacheService — no Spring context needed.
 * 2. The service can be swapped (e.g., a distributed service in Phase 6)
 *    without touching the controller.
 * 3. Spring AOP (e.g., transaction management, future metrics interception)
 *    works best on interfaces.
 *
 * <p>The service layer is responsible for:</p>
 * - Orchestrating business logic across the store
 * - Maintaining hit/miss statistics
 * - Translating between DTOs and domain models
 * - The controller should contain ZERO business logic
 */
public interface CacheService {

    /**
     * Store a key-value pair in the cache.
     * If the key already exists, the value is overwritten.
     *
     * @param request validated request DTO containing key and value
     * @return the stored entry as a response DTO
     */
    CacheEntryResponse put(CachePutRequest request);

    /**
     * Retrieve a value from the cache by key.
     *
     * @param key the key to look up
     * @return the entry as a response DTO
     * @throws com.cache.exception.CacheKeyNotFoundException if key does not exist
     */
    CacheEntryResponse get(String key);

    /**
     * Remove a specific key from the cache.
     *
     * @param key the key to remove
     * @throws com.cache.exception.CacheKeyNotFoundException if key does not exist
     */
    void delete(String key);

    /**
     * Remove all keys from the cache.
     *
     * @return number of entries that were removed
     */
    long clear();

    /**
     * Return operational statistics for this cache node.
     *
     * @return snapshot of current stats
     */
    CacheStatsResponse getStats();

    /**
     * Dynamically update the eviction policy strategy of this node.
     *
     * @param policyName name of the policy (e.g. LRU, LFU, FIFO, NO_EVICTION)
     */
    void updateEvictionPolicy(String policyName);
}
