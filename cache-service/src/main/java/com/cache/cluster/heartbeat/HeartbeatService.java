package com.cache.cluster.heartbeat;

import com.cache.cluster.model.NodeInfo;

/**
 * Contract for the heartbeat emitter — the component responsible for
 * periodically pinging all known peer nodes to confirm their liveness.
 *
 * <p><strong>Why a scheduled heartbeat instead of on-demand health checks?</strong></p>
 * Passive failure detection (only checking when a request fails) leads to long
 * periods where a dead node still receives traffic. An active heartbeat proactively
 * updates each node's {@link NodeInfo#getLastHeartbeatAt()} so the
 * {@link com.cache.cluster.health.FailureDetectorService} can transition
 * stale nodes through SUSPECT → DOWN without waiting for a user request.
 *
 * <p>Implementation is scheduled via {@code @Scheduled} with a fixed delay
 * driven by {@code cache.cluster.heartbeat-interval-ms}.</p>
 */
public interface HeartbeatService {

    /**
     * Emit heartbeats to all known peer nodes in the cluster registry.
     * <p>This method is called on a schedule. It iterates all registered nodes,
     * skips self, and calls {@link #pingNode(NodeInfo)} for each peer.
     * A successful response updates the node's {@code lastHeartbeatAt}.</p>
     */
    void emitHeartbeats();

    /**
     * Send a single liveness ping to the given node.
     *
     * @param node the peer node to ping
     * @return {@code true} if the node responded within the timeout; {@code false} otherwise
     */
    boolean pingNode(NodeInfo node);
}
