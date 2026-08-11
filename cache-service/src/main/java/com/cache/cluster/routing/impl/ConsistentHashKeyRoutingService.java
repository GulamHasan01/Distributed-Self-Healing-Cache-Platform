package com.cache.cluster.routing.impl;

import com.cache.cluster.routing.ClusterRingManager;
import com.cache.cluster.routing.ConsistentHashRing;
import com.cache.cluster.routing.KeyRoutingService;
import com.cache.config.CacheProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * {@link KeyRoutingService} backed by the {@link ClusterRingManager}'s cached ring.
 *
 * <p><strong>Phase 7 change from Phase 4:</strong></p>
 * In Phase 4, every call to {@code getOwnerNodeId()} and {@code getRouteList()} built
 * a brand-new {@link ConsistentHashRing} from scratch by querying the registry for all
 * UP nodes and hashing them into a {@code TreeMap}. This was correct but wasteful:
 * the ring topology only changes when a node joins, leaves, or changes health status.
 *
 * <p>In Phase 7 we inject {@link ClusterRingManager}, which holds a single
 * {@code volatile} ring reference. The ring is rebuilt only when cluster membership
 * changes (failure detection transitions, recovery, startup). Every routing call
 * is now a {@code volatile} read — O(1) with zero allocation on the hot path.</p>
 *
 * <p>Only {@code NodeStatus.UP} nodes are in the cached ring. When the failure
 * detector marks a node SUSPECT or DOWN, it triggers {@link ClusterRingManager#rebuildRing()},
 * which excludes that node. The next routing call automatically avoids it.</p>
 */
@Service
public class ConsistentHashKeyRoutingService implements KeyRoutingService {

    private static final Logger log = LoggerFactory.getLogger(ConsistentHashKeyRoutingService.class);

    private final ClusterRingManager clusterRingManager;
    private final CacheProperties cacheProperties;

    public ConsistentHashKeyRoutingService(ClusterRingManager clusterRingManager,
                                           CacheProperties cacheProperties) {
        this.clusterRingManager = clusterRingManager;
        this.cacheProperties = cacheProperties;
    }

    @Override
    public String getOwnerNodeId(String key) {
        ConsistentHashRing ring = clusterRingManager.getRing();

        String owner = ring.getNodeForKey(key)
                .orElse(cacheProperties.getNode().getId()); // fallback: handle locally

        log.debug("Hash-routing key='{}' -> owner='{}' (ring={} UP nodes)",
                key, owner, ring.size());
        return owner;
    }

    @Override
    public List<String> getRouteList(String key) {
        ConsistentHashRing ring = clusterRingManager.getRing();
        int replicationFactor = cacheProperties.getCluster().getReplicationFactor();

        List<String> routes = ring.getNodesForKey(key, replicationFactor);

        if (routes.isEmpty()) {
            routes.add(cacheProperties.getNode().getId());
        }
        return routes;
    }

    @Override
    public List<String> getReplicaNodeIds(String key) {
        List<String> routes = getRouteList(key);
        if (routes.size() <= 1) {
            return java.util.Collections.emptyList();
        }
        return routes.subList(1, routes.size());
    }
}