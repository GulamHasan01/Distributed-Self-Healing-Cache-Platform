package com.cache.cluster.replication;

import com.cache.dto.request.CachePutRequest;

/**
 * Orchestrates write replication to replica nodes in the cluster.
 *
 * <p>Phase 8 — Hardened Replication:</p>
 * <ul>
 *   <li>Async mode (default): replication runs off the HTTP request thread on a
 *       dedicated {@code replicationExecutor} thread pool. The client receives a
 *       response as soon as the primary write succeeds.</li>
 *   <li>Replica selection: only UP nodes are targeted. DOWN/SUSPECT nodes are
 *       skipped automatically.</li>
 *   <li>Fault isolation: a replica failure does not fail the primary write.
 *       Failures are counted and surfaced through {@link #getStats()}.</li>
 * </ul>
 */
public interface ReplicationService {

    /**
     * Asynchronously replicates a PUT to all UP replica nodes for the given key.
     *
     * <p>Returns immediately in async mode. Any per-replica failures are logged
     * as WARN and counted in {@link #getStats()} but never propagate to the caller.</p>
     *
     * @param request      the cache entry to replicate
     * @param replicaNodeIds ordered list of replica node IDs (from {@code KeyRoutingService.getReplicaNodeIds()})
     * @param sourceNodeId  ID of the node coordinating the replication (self)
     */
    void replicatePutAsync(CachePutRequest request, java.util.List<String> replicaNodeIds, String sourceNodeId);

    /**
     * Asynchronously replicates a DELETE to all UP replica nodes for the given key.
     *
     * <p>Returns immediately in async mode. Failures are counted but not thrown.</p>
     *
     * @param key          the key to delete from replicas
     * @param replicaNodeIds ordered list of replica node IDs
     * @param sourceNodeId  ID of the node coordinating the replication (self)
     */
    void replicateDeleteAsync(String key, java.util.List<String> replicaNodeIds, String sourceNodeId);

    /**
     * Returns a live snapshot of replication metrics since startup.
     *
     * @return immutable {@link ReplicationStats} snapshot
     */
    ReplicationStats getStats();
}
