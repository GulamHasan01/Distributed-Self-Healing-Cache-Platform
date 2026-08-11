package com.cache.cluster.routing;

import com.cache.cluster.model.NodeInfo;
import com.cache.cluster.model.NodeStatus;
import com.cache.cluster.registry.ClusterRegistry;
import com.cache.config.CacheProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collection;

/**
 * Manages a single cached {@link ConsistentHashRing} instance for the cluster.
 *
 * <p><strong>Why cache the ring?</strong></p>
 * In Phase 4, every routing request rebuilt the ring from scratch by querying
 * the registry for all UP nodes and constructing a new {@link ConsistentHashRing}.
 * Building a ring with 150 virtual nodes per physical node involves hashing and
 * inserting entries into a {@code TreeMap} — unnecessary work for every cache GET/PUT
 * when the ring only changes when a node joins, leaves, or changes health status.
 *
 * <p>This component holds a single {@code volatile ConsistentHashRing} that is
 * rebuilt only when cluster membership changes:</p>
 * <ul>
 *   <li>On startup (via {@link com.cache.cluster.startup.NodeStartupRegistrar})</li>
 *   <li>When the {@link com.cache.cluster.health.FailureDetectorService} transitions
 *       a node to SUSPECT or DOWN</li>
 *   <li>When a DOWN node recovers (heartbeat resumes or re-registration)</li>
 * </ul>
 *
 * <p><strong>Thread safety:</strong></p>
 * <ul>
 *   <li>{@code volatile} on {@code ring} ensures all threads always see the
 *       latest published ring without requiring synchronization on reads.</li>
 *   <li>{@code synchronized} on {@code rebuildRing()} prevents two concurrent
 *       topology changes from racing to publish different ring snapshots.</li>
 * </ul>
 */
@Component
public class ClusterRingManager {

    private static final Logger log = LoggerFactory.getLogger(ClusterRingManager.class);

    private final ClusterRegistry clusterRegistry;
    private final int virtualNodesPerNode;

    /**
     * The currently active ring. {@code volatile} so ring swaps are immediately
     * visible to all reader threads without synchronization on the hot path.
     */
    private volatile ConsistentHashRing ring;

    public ClusterRingManager(ClusterRegistry clusterRegistry,
                              CacheProperties cacheProperties) {
        this.clusterRegistry = clusterRegistry;
        this.virtualNodesPerNode = cacheProperties.getCluster().getVirtualNodesPerNode();
        // Build an initial empty ring — will be populated on first rebuildRing() call.
        this.ring = new ConsistentHashRing(java.util.Collections.emptyList(), virtualNodesPerNode);
    }

    /**
     * Returns the currently cached consistent hash ring.
     *
     * <p>This is the <strong>hot path</strong> — called on every cache GET/PUT routing
     * decision. It is an unsynchronized volatile read: O(1) with no locking.</p>
     *
     * @return the current {@link ConsistentHashRing}; never {@code null}
     */
    public ConsistentHashRing getRing() {
        return ring;
    }

    /**
     * Rebuilds the consistent hash ring from the current set of UP nodes in the registry.
     *
     * <p>Only UP nodes are added to the ring — SUSPECT and DOWN nodes are excluded
     * so no traffic is routed to them after failure detection marks them unhealthy.</p>
     *
     * <p>This method is {@code synchronized} to prevent concurrent rebuilds from
     * different scheduler threads overwriting each other's results. It is only called
     * on membership-change events (not on every request), so the synchronization
     * overhead is negligible.</p>
     */
    public synchronized void rebuildRing() {
        Collection<NodeInfo> upNodes = clusterRegistry.findByStatus(NodeStatus.UP);
        ConsistentHashRing newRing = new ConsistentHashRing(upNodes, virtualNodesPerNode);
        this.ring = newRing;
        log.info("Consistent hash ring rebuilt: {} UP node(s) on ring — {}",
                newRing.size(),
                upNodes.stream().map(NodeInfo::getId).toList());
    }
}
