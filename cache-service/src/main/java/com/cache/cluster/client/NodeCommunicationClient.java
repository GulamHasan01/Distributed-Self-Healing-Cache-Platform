package com.cache.cluster.client;

import com.cache.cluster.model.NodeInfo;
import com.cache.dto.request.CachePutRequest;
import com.cache.dto.response.CacheEntryResponse;

/**
 * Client for performing cache operations on a remote node in the cluster.
 *
 * <p>Handles HTTP calls to other nodes using REST client. Mapped to correct
 * remote endpoints: GET /api/v1/cache/{key}, PUT /api/v1/cache, etc.</p>
 */
public interface NodeCommunicationClient {

    /**
     * Retrieve a cache entry from a remote node.
     *
     * @param targetNode the remote node to query
     * @param key the cache key
     * @return the CacheEntryResponse
     * @throws com.cache.exception.CacheKeyNotFoundException if key is not found on remote node
     * @throws com.cache.cluster.exception.NodeCommunicationException if request fails
     */
    CacheEntryResponse get(NodeInfo targetNode, String key);

    /**
     * Store a cache entry on a remote node.
     *
     * @param targetNode the remote node to write to
     * @param request the write request
     * @return the CacheEntryResponse of the stored entry
     * @throws com.cache.exception.CacheCapacityExceededException if remote node cache is full
     * @throws com.cache.cluster.exception.NodeCommunicationException if request fails
     */
    CacheEntryResponse put(NodeInfo targetNode, CachePutRequest request);

    /**
     * Store a cache entry on a remote node as part of replication.
     *
     * @param targetNode the remote node to write to
     * @param request the write request
     * @param sourceNodeId the node that coordinates this replication (the primary owner)
     * @return the CacheEntryResponse of the stored entry
     * @throws com.cache.cluster.exception.NodeCommunicationException if request fails
     */
    CacheEntryResponse putReplicated(NodeInfo targetNode, CachePutRequest request, String sourceNodeId);

    /**
     * Delete a cache entry from a remote node.
     *
     * @param targetNode the remote node to delete from
     * @param key the cache key
     * @throws com.cache.exception.CacheKeyNotFoundException if key is not found on remote node
     * @throws com.cache.cluster.exception.NodeCommunicationException if request fails
     */
    void delete(NodeInfo targetNode, String key);

    /**
     * Delete a cache entry from a remote node as part of replication.
     *
     * @param targetNode the remote node to delete from
     * @param key the cache key
     * @param sourceNodeId the node that coordinates this replication (the primary owner)
     * @throws com.cache.cluster.exception.NodeCommunicationException if request fails
     */
    void deleteReplicated(NodeInfo targetNode, String key, String sourceNodeId);
}
