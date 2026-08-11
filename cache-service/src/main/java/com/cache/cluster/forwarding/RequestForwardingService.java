package com.cache.cluster.forwarding;

import com.cache.dto.request.CachePutRequest;
import com.cache.dto.response.CacheEntryResponse;

/**
 * Service responsible for forwarding cache requests to other nodes in the cluster.
 *
 * <p>Acts as an orchestrator that resolves the node ID to metadata and forwards the
 * operation using the communication client.</p>
 */
public interface RequestForwardingService {

    /**
     * Forward a GET request to a remote node.
     *
     * @param targetNodeId the ID of the remote node
     * @param key the cache key
     * @return the CacheEntryResponse from the remote node
     * @throws com.cache.cluster.exception.NodeNotFoundException if target node is not registered
     * @throws com.cache.cluster.exception.NodeCommunicationException if HTTP call fails
     */
    CacheEntryResponse forwardGet(String targetNodeId, String key);

    /**
     * Forward a PUT request to a remote node.
     *
     * @param targetNodeId the ID of the remote node
     * @param request the cache put request
     * @return the CacheEntryResponse from the remote node
     * @throws com.cache.cluster.exception.NodeNotFoundException if target node is not registered
     * @throws com.cache.cluster.exception.NodeCommunicationException if HTTP call fails
     */
    CacheEntryResponse forwardPut(String targetNodeId, CachePutRequest request);

    /**
     * Forward a DELETE request to a remote node.
     *
     * @param targetNodeId the ID of the remote node
     * @param key the cache key
     * @throws com.cache.cluster.exception.NodeNotFoundException if target node is not registered
     * @throws com.cache.cluster.exception.NodeCommunicationException if HTTP call fails
     */
    void forwardDelete(String targetNodeId, String key);

    /**
     * Send a replication PUT request to a backup replica node.
     *
     * @param targetNodeId the ID of the remote node to write to
     * @param request the cache put request
     * @param sourceNodeId the node coordinating the replication
     * @return the CacheEntryResponse from the remote node
     */
    CacheEntryResponse replicatePut(String targetNodeId, CachePutRequest request, String sourceNodeId);

    /**
     * Send a replication DELETE request to a backup replica node.
     *
     * @param targetNodeId the ID of the remote node to delete from
     * @param key the cache key
     * @param sourceNodeId the node coordinating the replication
     */
    void replicateDelete(String targetNodeId, String key, String sourceNodeId);
}
