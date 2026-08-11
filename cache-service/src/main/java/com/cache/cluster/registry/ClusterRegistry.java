package com.cache.cluster.registry;

import com.cache.cluster.model.NodeInfo;
import com.cache.cluster.model.NodeStatus;

import java.util.Collection;
import java.util.Optional;

/**
 * Contract for the cluster node registry — the authoritative store of all known nodes.
 *
 * <p>WHY a separate registry interface from the service?</p>
 * The registry is STORAGE — it knows HOW to store and retrieve nodes.
 * The service is ORCHESTRATION — it knows WHAT to do with nodes (business logic,
 * validation, event publishing in future phases).
 * Separating them follows the Single Responsibility Principle and makes the
 * storage layer independently mockable and replaceable.
 *
 * <p>In Phase 3, the implementation is in-memory ({@link InMemoryClusterRegistry}).
 * In a production system, this interface would be implemented with:</p>
 * <ul>
 *   <li>Redis (fast, already in our stack)</li>
 *   <li>A dedicated etcd cluster</li>
 *   <li>ZooKeeper</li>
 *   <li>Consul</li>
 * </ul>
 * The interface shields all callers from the storage technology choice.
 */
public interface ClusterRegistry {

    /**
     * Register a node in the cluster. If a node with the same ID already exists,
     * its entry is replaced (idempotent re-registration).
     *
     * @param node the node to register
     */
    void register(NodeInfo node);

    /**
     * Remove a node from the cluster registry.
     *
     * @param nodeId the ID of the node to deregister
     * @return true if the node existed and was removed, false if it wasn't found
     */
    boolean deregister(String nodeId);

    /**
     * Find a specific node by its ID.
     *
     * @param nodeId the node ID to look up
     * @return the NodeInfo if found, empty otherwise
     */
    Optional<NodeInfo> findById(String nodeId);

    /**
     * Retrieve all registered nodes.
     *
     * @return unmodifiable collection of all registered NodeInfo objects
     */
    Collection<NodeInfo> findAll();

    /**
     * Retrieve only nodes with a specific status.
     * Used in Phase 5 (consistent hashing only considers UP nodes).
     *
     * @param status the status to filter by
     * @return collection of nodes with the given status
     */
    Collection<NodeInfo> findByStatus(NodeStatus status);

    /**
     * Check if a node with the given ID exists in the registry.
     *
     * @param nodeId the node ID to check
     * @return true if registered
     */
    boolean exists(String nodeId);

    /**
     * Total number of registered nodes (all statuses).
     *
     * @return node count
     */
    int size();
}
