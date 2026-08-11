package com.cache.cluster.heartbeat.impl;

import com.cache.cluster.heartbeat.HeartbeatService;
import com.cache.cluster.model.NodeInfo;
import com.cache.cluster.registry.ClusterRegistry;
import com.cache.cluster.service.ClusterRegistryService;
import com.cache.config.CacheProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * HTTP-based implementation of {@link HeartbeatService}.
 *
 * <p>On each scheduled tick this service:</p>
 * <ol>
 *   <li>Iterates all nodes registered in the {@link ClusterRegistry}.</li>
 *   <li>Skips self (node whose ID matches {@code cache.node.id}).</li>
 *   <li>Calls {@code GET <nodeBaseUrl>/api/v1/cluster/health/ping} on each peer.</li>
 *   <li>On HTTP 200 → calls {@link ClusterRegistryService#recordHeartbeat(String)}
 *       to update {@code lastHeartbeatAt} and heal any SUSPECT nodes.</li>
 *   <li>On timeout or error → logs a warning (the failure detector handles state transitions).</li>
 * </ol>
 *
 * <p><strong>Why a dedicated heartbeatRestClient?</strong></p>
 * The heartbeat client uses a {@code heartbeatTimeoutMs} timeout, much shorter than the
 * general {@code readTimeoutMs}. A slow heartbeat response is treated the same as no response —
 * we don't want the heartbeat thread blocked indefinitely waiting for an overloaded peer.
 *
 * <p><strong>Scheduling:</strong></p>
 * Uses {@code @Scheduled(fixedDelayString = ...)} with a property placeholder so the interval
 * is driven purely by configuration. {@code fixedDelay} ensures the next tick starts only
 * after the previous round completes, preventing heartbeat floods if peers are slow.
 */
@Service
public class HttpHeartbeatService implements HeartbeatService {

    private static final Logger log = LoggerFactory.getLogger(HttpHeartbeatService.class);

    private static final String PING_PATH = "/api/v1/cluster/health/ping";

    private final ClusterRegistry clusterRegistry;
    private final ClusterRegistryService clusterRegistryService;
    private final CacheProperties cacheProperties;
    private final RestClient heartbeatClient;

    public HttpHeartbeatService(ClusterRegistry clusterRegistry,
                                ClusterRegistryService clusterRegistryService,
                                CacheProperties cacheProperties,
                                @Qualifier("heartbeatRestClient") RestClient heartbeatClient) {
        this.clusterRegistry = clusterRegistry;
        this.clusterRegistryService = clusterRegistryService;
        this.cacheProperties = cacheProperties;
        this.heartbeatClient = heartbeatClient;
    }

    /**
     * Scheduled heartbeat round — runs every {@code cache.cluster.heartbeat-interval-ms} ms.
     * Uses a property placeholder so it's configurable without code changes.
     */
    @Scheduled(fixedDelayString = "${cache.cluster.heartbeat-interval-ms:5000}")
    @Override
    public void emitHeartbeats() {
        String selfId = cacheProperties.getNode().getId();

        clusterRegistry.findAll().forEach(node -> {
            // Never ping ourselves — we know we're alive
            if (selfId.equals(node.getId())) {
                return;
            }
            boolean alive = pingNode(node);
            if (alive) {
                try {
                    clusterRegistryService.recordHeartbeat(node.getId());
                    log.debug("Heartbeat OK: node='{}' at {}", node.getId(), node.getBaseUrl());
                } catch (Exception e) {
                    log.warn("Failed to record heartbeat for node='{}': {}", node.getId(), e.getMessage());
                }
            } else {
                log.warn("Heartbeat MISSED: node='{}' at {} did not respond",
                        node.getId(), node.getBaseUrl());
            }
        });
    }

    /**
     * Sends a single GET /ping request to the given node.
     *
     * @param node the peer node to ping
     * @return {@code true} if HTTP 200 received, {@code false} on any error or timeout
     */
    @Override
    public boolean pingNode(NodeInfo node) {
        try {
            heartbeatClient.get()
                    .uri(node.getBaseUrl() + PING_PATH)
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (Exception e) {
            log.trace("Ping failed for node='{}': {}", node.getId(), e.getMessage());
            return false;
        }
    }
}
