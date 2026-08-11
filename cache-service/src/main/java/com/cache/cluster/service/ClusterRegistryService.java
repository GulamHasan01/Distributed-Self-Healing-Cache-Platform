package com.cache.cluster.service;

import com.cache.cluster.model.NodeInfo;
import com.cache.cluster.model.NodeStatus;
import com.cache.dto.request.NodeRegistrationRequest;
import com.cache.dto.response.ClusterStatusResponse;
import com.cache.dto.response.NodeInfoResponse;

import java.util.List;

/**
 * Business logic interface for cluster node management.
 *
 * <p>WHY separate service from registry?</p>
 * The registry is raw storage — no business logic.
 * The service is where policy decisions live:
 * <ul>
 *   <li>Should re-registration be allowed? Under what conditions?</li>
 *   <li>When a node registers, should an event be published? (Phase 11)</li>
 *   <li>When a node deregisters, should in-flight rebalancing trigger? (Phase 9)</li>
 * </ul>
 * The controller calls the service; the service calls the registry.
 * Three layers, each with one responsibility.
 */
public interface ClusterRegistryService {

    /**
     * Register a new node or update an existing node's metadata.
     * Registration is idempotent: registering the same nodeId again
     * replaces the previous registration.
     *
     * @param request validated registration details
     * @return the NodeInfoResponse for the registered node
     */
    NodeInfoResponse register(NodeRegistrationRequest request);

    /**
     * Deregister a node from the cluster.
     *
     * @param nodeId the ID of the node to remove
     * @throws com.cache.cluster.exception.NodeNotFoundException if no node with that ID exists
     */
    void deregister(String nodeId);

    /**
     * Look up a specific node by ID.
     *
     * @param nodeId the node ID to retrieve
     * @return NodeInfoResponse for the node
     * @throws com.cache.cluster.exception.NodeNotFoundException if not found
     */
    NodeInfoResponse getNode(String nodeId);

    /**
     * List all registered nodes.
     *
     * @return list of NodeInfoResponse for all nodes in any status
     */
    List<NodeInfoResponse> getAllNodes();

    /**
     * List all nodes in a specific status.
     *
     * @param status the status to filter by
     * @return list of nodes in that status
     */
    List<NodeInfoResponse> getNodesByStatus(NodeStatus status);

    /**
     * Get a comprehensive cluster health summary.
     *
     * @return ClusterStatusResponse with aggregate counts and full node list
     */
    ClusterStatusResponse getClusterStatus();

    /**
     * Mark a node as UP (node has finished starting).
     * Called by the startup registrar after registration completes.
     *
     * @param nodeId the node to mark as UP
     * @throws com.cache.cluster.exception.NodeNotFoundException if not found
     */
    void markNodeUp(String nodeId);

    /**
     * Record a successful heartbeat for the given node.
     *
     * <p>Updates {@code lastHeartbeatAt} to now. If the node was in SUSPECT state,
     * it transitions back to UP (self-healing recovery). The caller is responsible
     * for triggering a ring rebuild if the status changed.
     *
     * <p>Called by the heartbeat emitter after a successful /ping response from a peer.</p>
     *
     * @param nodeId the node that successfully responded to a heartbeat ping
     * @throws com.cache.cluster.exception.NodeNotFoundException if no node with that ID exists
     */
    void recordHeartbeat(String nodeId);

    /**
     * Retrieves the current virtual node mapping from the active consistent hash ring.
     */
    List<com.cache.dto.response.VirtualNodeResponse> getRingMapping();

    /**
     * Manually updates a node's status (used for simulating cluster failures).
     */
    NodeInfoResponse updateNodeStatus(String nodeId, NodeStatus status);
}
