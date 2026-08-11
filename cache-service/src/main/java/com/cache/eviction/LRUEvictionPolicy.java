package com.cache.eviction;

import com.cache.model.CacheEntry;

import java.util.Collection;
import java.util.Comparator;
import java.util.Optional;

/**
 * Least Recently Used (LRU) eviction policy.
 *
 * <p>Algorithm:</p>
 * <pre>
 * Given: [A(accessed=t1), B(accessed=t4), C(accessed=t2)]
 * Sort by lastAccessed ascending → [A(t1), C(t2), B(t4)]
 * Select minimum → A (accessed longest ago)
 * Evict A
 * </pre>
 *
 * <p>Time Complexity: O(n) — we scan all entries to find the minimum.</p>
 *
 * <p>WHY O(n) and not O(1)?</p>
 * A true O(1) LRU requires a doubly-linked list + HashMap (the classic LRU Cache
 * LeetCode problem). This gives O(1) put AND O(1) eviction.
 *
 * However, our ConcurrentHashMap-based store doesn't maintain insertion order,
 * so we can't use that approach directly. For Phase 2 we use O(n) because:
 * <ol>
 *   <li>Evictions are RARE — only triggered when the cache is 100% full</li>
 *   <li>The store is the hot path, not eviction</li>
 *   <li>For 10,000 entries, O(n) is still microseconds</li>
 *   <li>In Phase 6, the entire store is redesigned for distribution anyway</li>
 * </ol>
 *
 * <p>For a production system requiring true O(1) LRU, you would use
 * {@code LinkedHashMap} with access order enabled, protected by
 * a {@code ReentrantReadWriteLock}. That approach is mentioned in Phase notes
 * but not used here to keep the store thread-safe with ConcurrentHashMap.</p>
 */
public class LRUEvictionPolicy implements EvictionPolicy {

    @Override
    public Optional<String> selectVictim(Collection<CacheEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            return Optional.empty();
        }

        // Find the entry with the oldest lastAccessed timestamp.
        // Stream.min returns Optional<CacheEntry>, then we extract the key.
        return entries.stream()
                .min(Comparator.comparing(CacheEntry::getLastAccessed))
                .map(CacheEntry::getKey);
    }

    @Override
    public String policyName() {
        return "LRU";
    }
}
