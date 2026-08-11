package com.cache.eviction;

import com.cache.model.CacheEntry;

import java.util.Collection;
import java.util.Optional;

/**
 * No-eviction policy — maintains Phase 1 behavior.
 *
 * <p>When selected, the store will NEVER evict entries automatically.
 * A PUT on a full cache results in a {@link com.cache.exception.CacheCapacityExceededException}.</p>
 *
 * <p>Use case: When you need strict capacity enforcement — for example,
 * in a billing system where cache overflow must surface as a hard error
 * rather than silently discarding data.</p>
 *
 * <p>selectVictim always returns empty Optional, which signals the store
 * to throw the capacity exception instead of evicting.</p>
 */
public class NoEvictionPolicy implements EvictionPolicy {

    @Override
    public Optional<String> selectVictim(Collection<CacheEntry> entries) {
        // Explicitly return empty → the store knows to reject the PUT
        return Optional.empty();
    }

    @Override
    public String policyName() {
        return "NO_EVICTION";
    }
}
