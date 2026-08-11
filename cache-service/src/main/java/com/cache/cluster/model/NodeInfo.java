package com.cache.cluster.model;

import java.time.Instant;

/**
 * Domain model representing a single cache node in the cluster.
 *
 * <p>Each node that joins the cluster registers a {@code NodeInfo} record.
 * This is the single source of truth for a node's identity and status.</p>
 *
 * <p>WHY a mutable class (not a record)?</p>
 * Records in Java are immutable. Node status and heartbeat timestamps MUST change
 * over the lifetime of a node (UP → SUSPECT → DOWN → UP after recovery).
 * Using a mutable class allows in-place updates without replacing the entire object
 * in the registry — important for atomicity with {@code synchronized} blocks in Phase 7.
 *
 * <p>Thread-safety note:</p>
 * {@code volatile} is applied to mutable fields ({@code status}, {@code lastHeartbeatAt})
 * to guarantee that updates made by the heartbeat thread are immediately visible to
 * reader threads without full synchronization overhead.
 */
public class NodeInfo {

    private final String id;
    private final String host;
    private final int port;
    private final Instant registeredAt;

    /** Status is mutable — changes as node health is observed. */
    private volatile NodeStatus status;

    /**
     * Timestamp of last confirmed heartbeat from this node.
     * Used in Phase 7 (failure detection) to determine if a node has gone SUSPECT.
     * Initialized to registeredAt to avoid false SUSPECT detection at startup.
     */
    private volatile Instant lastHeartbeatAt;

    public NodeInfo(String id, String host, int port) {
        this.id = id;
        this.host = host;
        this.port = port;
        this.registeredAt = Instant.now();
        this.status = NodeStatus.STARTING;
        this.lastHeartbeatAt = Instant.now();
    }

    // -------------------------------------------------------------------------
    // Business methods
    // -------------------------------------------------------------------------

    /**
     * Marks this node as UP and refreshes its heartbeat timestamp.
     * Called when a node confirms readiness after registration,
     * or after recovering from SUSPECT/DOWN.
     */
    public void markUp() {
        this.status = NodeStatus.UP;
        this.lastHeartbeatAt = Instant.now();
    }

    /**
     * Records a heartbeat from this node, refreshing the last-seen timestamp.
     * Status transitions from SUSPECT → UP if the node was suspected.
     * Used in Phase 7 (Heartbeat).
     */
    public void recordHeartbeat() {
        this.lastHeartbeatAt = Instant.now();
        if (this.status == NodeStatus.SUSPECT || this.status == NodeStatus.STARTING || this.status == NodeStatus.DOWN) {
            this.status = NodeStatus.UP;
        }
    }

    /**
     * Returns the base URL of this node (e.g., http://node-2:8082).
     * Used in Phase 4 when nodes communicate with each other via WebClient.
     */
    public String getBaseUrl() {
        return "http://" + host + ":" + port;
    }

    /**
     * Returns true if this node is available to serve cache traffic.
     * Only UP nodes should receive routing requests.
     */
    public boolean isAvailable() {
        return status == NodeStatus.UP;
    }

    // -------------------------------------------------------------------------
    // Getters / Setters
    // -------------------------------------------------------------------------

    public String getId() { return id; }
    public String getHost() { return host; }
    public int getPort() { return port; }
    public Instant getRegisteredAt() { return registeredAt; }
    public NodeStatus getStatus() { return status; }
    public void setStatus(NodeStatus status) { this.status = status; }
    public Instant getLastHeartbeatAt() { return lastHeartbeatAt; }

    @Override
    public String toString() {
        return "NodeInfo{id='" + id + "', host='" + host + "', port=" + port +
               ", status=" + status + ", registeredAt=" + registeredAt + "}";
    }
}
