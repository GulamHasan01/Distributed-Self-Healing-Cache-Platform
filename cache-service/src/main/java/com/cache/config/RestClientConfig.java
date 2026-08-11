package com.cache.config;

import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Spring configuration for the {@link RestClient} used in inter-node HTTP communication.
 *
 * <p>WHY configure timeouts?</p>
 * Without timeouts, a single unresponsive remote node can block a thread indefinitely.
 * In a cache cluster with 10 nodes, if 3 nodes are slow, you could exhaust your entire
 * HTTP thread pool waiting for them. Timeouts are the first line of defense against
 * cascading failures — a concept known as "bulkhead" in resilience engineering.
 *
 * <p>WHY use a {@link SimpleClientHttpRequestFactory} (not HttpComponentsClientHttpRequestFactory)?</p>
 * {@code SimpleClientHttpRequestFactory} wraps Java's built-in {@code HttpURLConnection}.
 * It requires ZERO additional dependencies. For a teaching project with low-to-medium
 * concurrency, it is perfectly adequate. In production at Netflix scale, you would use
 * Apache HttpComponents or OkHttp for connection pooling and better concurrency control.
 *
 * <p>Configuration is read from {@link CacheProperties.ClusterProperties} so no values
 * are hard-coded here.</p>
 */
@Configuration
public class RestClientConfig {

    private final CacheProperties cacheProperties;

    public RestClientConfig(CacheProperties cacheProperties) {
        this.cacheProperties = cacheProperties;
    }

    /**
     * Creates the RestClient bean for inter-node communication.
     *
     * <p>The RestClient is thread-safe and can be shared across all callers.
     * Think of it like a configured HTTP connection factory — you call it
     * with a specific URL at request time, not at construction time.</p>
     *
     * <p>This bean will be injected into {@link com.cache.cluster.client.impl.RestClientNodeCommunicationClient}.
     * It is NOT the same as the RestClient a controller uses to respond — it is
     * specifically configured for outbound calls TO other cache nodes.</p>
     */
    @Bean("nodeRestClient")
    public RestClient nodeRestClient() {
        CacheProperties.ClusterProperties clusterConfig = cacheProperties.getCluster();

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(clusterConfig.getConnectTimeoutMs());
        factory.setReadTimeout(clusterConfig.getReadTimeoutMs());

        return RestClient.builder()
                .requestFactory(factory)
                .defaultHeader("Content-Type", "application/json")
                .defaultHeader("Accept", "application/json")
                // Identify outbound calls from this node — useful for distributed tracing (Phase 10)
                .defaultHeader("X-Cache-Node-Id", cacheProperties.getNode().getId())
                .requestInterceptor(new com.cache.observability.TraceInterceptor())
                .build();
    }

    /**
     * A dedicated RestClient for Phase 7 heartbeat pings with a shorter timeout.
     *
     * <p>WHY a separate bean from {@code nodeRestClient}?</p>
     * Heartbeat pings use a much shorter timeout ({@code heartbeatTimeoutMs}) than
     * the general cache data operations ({@code readTimeoutMs}). A missed heartbeat
     * must be detected quickly — we don't want the heartbeat thread blocked for 5 seconds
     * waiting for a dead node when the interval is only 5 seconds.
     * Keeping them separate avoids interference between data-plane and control-plane traffic.
     */
    @Bean("heartbeatRestClient")
    public RestClient heartbeatRestClient() {
        CacheProperties.ClusterProperties clusterConfig = cacheProperties.getCluster();

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) clusterConfig.getHeartbeatTimeoutMs());
        factory.setReadTimeout((int) clusterConfig.getHeartbeatTimeoutMs());

        return RestClient.builder()
                .requestFactory(factory)
                .defaultHeader("Content-Type", "application/json")
                .defaultHeader("Accept", "application/json")
                .defaultHeader("X-Cache-Node-Id", cacheProperties.getNode().getId())
                .build();
    }
}
