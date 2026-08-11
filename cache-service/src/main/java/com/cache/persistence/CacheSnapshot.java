package com.cache.persistence;

import java.time.Instant;
import java.util.List;

/**
 * Domain record representing a disk snapshot of the cache store.
 *
 * @param nodeNodeId ID of the node that created this snapshot
 * @param timestamp  When the snapshot was saved to disk
 * @param entries    List of non-expired cache records
 */
public record CacheSnapshot(
        String nodeNodeId,
        Instant timestamp,
        List<SnapshotEntry> entries
) {
    public record SnapshotEntry(
            String key,
            String value,
            Instant createdAt,
            long ttlSeconds
    ) {}
}
