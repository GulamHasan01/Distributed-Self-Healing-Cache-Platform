package com.cache.eviction;

import com.cache.model.CacheEntry;

import java.util.Collection;
import java.util.Optional;

/**
 * Strategy interface for cache eviction policies.
 *
 * <p>WHY the Strategy pattern here?</p>
 * The eviction algorithm is a dimension of behavior that varies independently
 * from the store implementation. Using a strategy:
 * <ul>
 *   <li>The store doesn't need to know which algorithm to use</li>
 *   <li>New policies (LFU, FIFO, Random) can be added without touching the store</li>
 *   <li>Policies are independently testable</li>
 *   <li>The active policy is selected at runtime via configuration</li>
 * </ul>
 *
 * <p>This is the Open/Closed Principle: the store is OPEN for extension
 * (new eviction policies) but CLOSED for modification.</p>
 *
 * <p>Future eviction strategies:</p>
 * <ul>
 *   <li>LFU (Least Frequently Used) — evict lowest access count</li>
 *   <li>FIFO (First In First Out) — evict oldest created entry</li>
 *   <li>Random — evict random entry (simple, surprisingly effective)</li>
 *   <li>TTL-aware — evict entries closest to expiry first</li>
 * </ul>
 */
public interface EvictionPolicy {

    /**
     * Select a key to evict from the current set of entries.
     *
     * @param entries current entries in the cache store (read-only view)
     * @return the key of the entry to evict, or empty if no eviction should occur
     */
    Optional<String> selectVictim(Collection<CacheEntry> entries);

    /**
     * Human-readable name of this eviction policy.
     * Used in stats and log messages.
     *
     * @return policy name
     */
    String policyName();
}
