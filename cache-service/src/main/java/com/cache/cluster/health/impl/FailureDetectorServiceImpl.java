package com.cache.cluster.health.impl;

import com.cache.cluster.health.FailureDetectorService;
import com.cache.cluster.model.NodeInfo;
import com.cache.cluster.model.NodeStatus;
import com.cache.cluster.registry.ClusterRegistry;
import com.cache.cluster.routing.ClusterRingManager;
import com.cache.config.CacheProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;

/**
 * Core self-healing component — implements the {@code UP → SUSPECT → DOWN} state machine.
 *
 * <p><strong>State transition logic (per node, per detection cycle):</strong></p>
 * <pre>
 *   age = now - node.lastHeartbeatAt
 *
 *   age &gt; downThresholdMs  AND status != DOWN    → mark DOWN,    rebuild ring
 *   age &gt; suspectThresholdMs AND status == UP    → mark SUSPECT, rebuild ring
 *   age &lt;= suspectThresholdMs AND status == SUSPECT → (heartbeat emitter handles UP recovery)
 * </pre>
 *
 * <p><strong>Why DOWN before SUSPECT in the check order?</strong></p>
 * A node can go from UP directly to DOWN if the detection cycle hasn't run between
 * the two thresholds (e.g. the service was paused or slow). Checking DOWN first ensures
 * a long-absent node gets marked DOWN immediately rather than landing in SUSPECT first.
 *
 * <p><strong>Scheduling:</strong></p>
 * Runs every {@code suspectThresholdMs / 2} milliseconds using Spring's
 * {@code @Scheduled} with a SpEL expression. This means a node is checked
 * roughly twice per suspect window, giving prompt detection while avoiding
 * unnecessary CPU churn.
 *
 * <p>Self is excluded from checks — a node never marks itself DOWN.
 * Node recovery (DOWN → UP) is handled by {@link com.cache.cluster.service.ClusterRegistryService#recordHeartbeat(String)}
 * which transitions SUSPECT → UP on a successful heartbeat ping.</p>
 */
@Service
public class FailureDetectorServiceImpl implements FailureDetectorService {

    private static final Logger log = LoggerFactory.getLogger(FailureDetectorServiceImpl.class);

    private final ClusterRegistry clusterRegistry;
    private final ClusterRingManager clusterRingManager;
    private final CacheProperties cacheProperties;

    public FailureDetectorServiceImpl(ClusterRegistry clusterRegistry,
                                      ClusterRingManager clusterRingManager,
                                      CacheProperties cacheProperties) {
        this.clusterRegistry = clusterRegistry;
        this.clusterRingManager = clusterRingManager;
        this.cacheProperties = cacheProperties;
    }

    /**
     * Scheduled detection cycle.
     *
     * <p>Runs every {@code suspectThresholdMs / 2} ms so nodes are detected promptly.
     * Uses {@code fixedDelay} so cycles don't overlap if the registry is large.</p>
     */
    @Scheduled(fixedDelayString = "${cache.cluster.suspect-threshold-ms:10000}")
    @Override
    public void runDetectionCycle() {
        String selfId = cacheProperties.getNode().getId();
        long suspectThresholdMs = cacheProperties.getCluster().getSuspectThresholdMs();
        long downThresholdMs    = cacheProperties.getCluster().getDownThresholdMs();

        Collection<NodeInfo> allNodes = clusterRegistry.findAll();
        boolean ringDirty = false;

        for (NodeInfo node : allNodes) {
            // Never self-diagnose — this node is clearly alive
            if (selfId.equals(node.getId())) {
                continue;
            }

            long ageMs = Duration.between(node.getLastHeartbeatAt(), Instant.now()).toMillis();
            NodeStatus currentStatus = node.getStatus();

            if (ageMs > downThresholdMs && currentStatus != NodeStatus.DOWN) {
                // -------------------------------------------------------
                // Transition to DOWN — no heartbeat for too long
                // -------------------------------------------------------
                node.setStatus(NodeStatus.DOWN);
                ringDirty = true;
                log.warn("NODE DOWN  : id='{}' — no heartbeat for {}ms (threshold={}ms). " +
                         "Removing from routing ring.",
                        node.getId(), ageMs, downThresholdMs);

            } else if (ageMs > suspectThresholdMs && currentStatus == NodeStatus.UP) {
                // -------------------------------------------------------
                // Transition to SUSPECT — missed heartbeat window, but not confirmed dead
                // -------------------------------------------------------
                node.setStatus(NodeStatus.SUSPECT);
                ringDirty = true;
                log.warn("NODE SUSPECT: id='{}' — heartbeat stale by {}ms (threshold={}ms). " +
                         "Removing from routing ring until confirmed alive.",
                        node.getId(), ageMs, suspectThresholdMs);

            } else {
                log.trace("Node '{}' is {} — heartbeat age={}ms", node.getId(), currentStatus, ageMs);
            }
        }

        // Only rebuild the ring if at least one node changed status
        if (ringDirty) {
            log.info("Cluster topology changed — rebuilding consistent hash ring.");
            clusterRingManager.rebuildRing();
        }
    }
}
