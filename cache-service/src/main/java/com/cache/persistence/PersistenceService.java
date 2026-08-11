package com.cache.persistence;

import com.cache.model.CacheEntry;

import java.util.Collection;
import java.util.List;

/**
 * Service interface for disk persistence and snapshotting.
 */
public interface PersistenceService {

    /**
     * Write current non-expired cache entries to disk atomically.
     *
     * @param entries collection of active cache entries to persist
     * @return number of entries successfully saved in the snapshot
     */
    int saveSnapshot(Collection<CacheEntry> entries);

    /**
     * Load non-expired snapshot entries from disk on startup.
     *
     * @return list of recovered cache entries
     */
    List<CacheEntry> loadSnapshot();
}
