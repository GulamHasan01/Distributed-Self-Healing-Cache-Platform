package com.cache.cluster.routing;

import java.util.List;

/**
 * Determines which cluster node should own a given cache key.
 *
 * <p>Implementations query the consistent hash ring built from the current
 * set of UP nodes in the cluster registry.</p>
 *
 * <p>This interface is the single point of contact for all routing decisions.
 * Controllers and services should NEVER build or query the ring directly.</p>
 */
public interface KeyRoutingService {

    /**
     * Returns the node ID that owns the given cache key according to the
     * current consistent hash ring.
     *
     * <p>Returns this node's own ID when the ring is empty (no UP nodes)
     * or when this node is the sole UP node — so callers always get a valid
     * target and can decide to handle locally.</p>
     *
     * @param key the cache key to route
     * @return the node ID of the key's owner (never null)
     */
    String getOwnerNodeId(String key);

    /**
     * Returns the replica node IDs (excluding the primary owner) that should
     * hold backup copies of the given cache key.
     *
     * @param key the cache key
     * @return a list of replica node IDs (empty if replication factor is 1 or no other nodes are available)
     */
    List<String> getReplicaNodeIds(String key);

    /**
     * Returns the full route path for the key, starting with the primary owner
     * followed by backup replica nodes.
     *
     * @param key the cache key
     * @return list of node IDs (primary first, then replicas)
     */
    List<String> getRouteList(String key);
}