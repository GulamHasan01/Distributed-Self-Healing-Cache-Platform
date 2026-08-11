package com.cache.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Strongly-typed configuration properties for the Cache Service.
 */
@Validated
@ConfigurationProperties(prefix = "cache")
public class CacheProperties {

    @Min(value = 1, message = "Cache max-size must be at least 1")
    private int maxSize = 10000;

    /**
     * Eviction policy applied when the store reaches max capacity.
     * Defaults to LRU for production use.
     */
    @NotNull
    private EvictionPolicyType evictionPolicy = EvictionPolicyType.LRU;

    @NotNull
    private TtlProperties ttl = new TtlProperties();

    @NotNull
    private NodeProperties node = new NodeProperties();

    @NotNull
    private ClusterProperties cluster = new ClusterProperties();

    @NotNull
    private ReplicationProperties replication = new ReplicationProperties();

    @NotNull
    private PersistenceProperties persistence = new PersistenceProperties();



    public enum EvictionPolicyType {
        /** Evict the entry that was accessed least recently. */
        LRU,
        /** Reject PUT when full — no eviction. */
        NO_EVICTION
    }



    public int getMaxSize() { return maxSize; }
    public void setMaxSize(int maxSize) { this.maxSize = maxSize; }

    public EvictionPolicyType getEvictionPolicy() { return evictionPolicy; }
    public void setEvictionPolicy(EvictionPolicyType evictionPolicy) { this.evictionPolicy = evictionPolicy; }

    public TtlProperties getTtl() { return ttl; }
    public void setTtl(TtlProperties ttl) { this.ttl = ttl; }

    public NodeProperties getNode() { return node; }
    public void setNode(NodeProperties node) { this.node = node; }

    public ClusterProperties getCluster() { return cluster; }
    public void setCluster(ClusterProperties cluster) { this.cluster = cluster; }

    public ReplicationProperties getReplication() { return replication; }
    public void setReplication(ReplicationProperties replication) { this.replication = replication; }

    public PersistenceProperties getPersistence() { return persistence; }
    public void setPersistence(PersistenceProperties persistence) { this.persistence = persistence; }

    // Persistence configuration

    public static class PersistenceProperties {

        /**
         * Whether RDB-style snapshot persistence to disk is enabled.
         * Default: true.
         */
        private boolean enabled = true;

        /**
         * Path to the snapshot file.
         * Default: "./data/cache-snapshot.json".
         */
        @NotBlank(message = "Snapshot filePath must not be blank")
        private String filePath = "./data/cache-snapshot.json";

        /**
         * Interval in seconds between periodic background snapshots.
         * Default: 60 seconds.
         */
        @Min(value = 1, message = "Snapshot interval must be at least 1 second")
        private long snapshotIntervalSeconds = 60;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }

        public String getFilePath() { return filePath; }
        public void setFilePath(String filePath) { this.filePath = filePath; }

        public long getSnapshotIntervalSeconds() { return snapshotIntervalSeconds; }
        public void setSnapshotIntervalSeconds(long snapshotIntervalSeconds) { this.snapshotIntervalSeconds = snapshotIntervalSeconds; }
    }

    // TTL configuration

    public static class TtlProperties {

        /**
         * Default TTL in seconds applied to entries that don't specify one.
         * -1 means "no default TTL" — entries are immortal unless the request provides a TTL.
         */
        private long defaultSeconds = -1;

        /**
         * How often (in milliseconds) the background expiration sweeper runs.
         * Lower values = faster expiration but more CPU overhead.
         * Default: 5000ms (5 seconds).
         */
        @Min(value = 100, message = "Sweep interval must be at least 100ms")
        private long sweepIntervalMs = 5000;

        public long getDefaultSeconds() { return defaultSeconds; }
        public void setDefaultSeconds(long defaultSeconds) { this.defaultSeconds = defaultSeconds; }

        public long getSweepIntervalMs() { return sweepIntervalMs; }
        public void setSweepIntervalMs(long sweepIntervalMs) { this.sweepIntervalMs = sweepIntervalMs; }
    }

    // Node configuration

    public static class NodeProperties {

        @NotBlank(message = "Node id must not be blank")
        private String id = "node-1";

        @NotBlank(message = "Node host must not be blank")
        private String host = "localhost";

        @Min(value = 1, message = "Node port must be >= 1")
        private int port = 8081;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getHost() { return host; }
        public void setHost(String host) { this.host = host; }

        public int getPort() { return port; }
        public void setPort(int port) { this.port = port; }
    }

    // Cluster communication configuration

    public static class ClusterProperties {

        /**
         * Max time in milliseconds to wait when establishing a connection to a remote node.
         * If a node is unreachable, the connection will fail fast after this timeout
         * rather than hanging indefinitely.
         * Default: 2000ms
         */
        @Min(value = 100, message = "Connect timeout must be at least 100ms")
        private int connectTimeoutMs = 2000;

        /**
         * Max time in milliseconds to wait for a response body from a remote node.
         * If the remote node is slow (GC pause, high load), we'll get a timeout
         * error rather than blocking the caller's thread forever.
         * Default: 5000ms
         */
        @Min(value = 100, message = "Read timeout must be at least 100ms")
        private int readTimeoutMs = 5000;

        /**
         * Number of additional attempts after the first failure.
         * 0 = no retries (fail immediately on first error).
         * Default: 2 retries.
         */
        @Min(value = 0, message = "Max retries must be >= 0")
        private int maxRetries = 2;

        /**
         * Number of virtual nodes placed on the consistent hash ring per physical node.
         * Higher values = more even key distribution across nodes, at the cost of
         * a larger ring data structure.
         * Default: 150 — a good balance for clusters of 2–20 nodes.
         */
        @Min(value = 1, message = "Virtual nodes per node must be at least 1")
        private int virtualNodesPerNode = 150;

        /**
         * Replication factor for cache entries in the cluster.
         * 1 = no replication (primary only)
         * 2 = 1 primary, 1 replica (default)
         * 3 = 1 primary, 2 replicas
         */
        @Min(value = 1, message = "Replication factor must be at least 1")
        private int replicationFactor = 2;

        // Heartbeat Emitter

        /**
         * How often (in milliseconds) this node sends a heartbeat ping to all known peers.
         * Lower = faster failure detection, higher = less network overhead.
         * Default: 5000ms (5 seconds).
         */
        @Min(value = 500, message = "Heartbeat interval must be at least 500ms")
        private long heartbeatIntervalMs = 5000;

        /**
         * Max time (in milliseconds) to wait for a peer's /ping response.
         * A peer that doesn't respond within this window counts as a missed heartbeat.
         * Should be less than heartbeatIntervalMs.
         * Default: 2000ms.
         */
        @Min(value = 100, message = "Heartbeat timeout must be at least 100ms")
        private long heartbeatTimeoutMs = 2000;

        // Failure Detection Thresholds

        /**
         * If a node's last successful heartbeat is older than this (ms), it is
         * transitioned from UP → SUSPECT. This is a "soft" failure signal —
         * the node may be overloaded or experiencing a brief network blip.
         * Default: 10000ms (2 missed heartbeat cycles).
         */
        @Min(value = 1000, message = "Suspect threshold must be at least 1000ms")
        private long suspectThresholdMs = 10000;

        /**
         * If a node's last successful heartbeat is older than this (ms), it is
         * transitioned from SUSPECT → DOWN. Traffic is no longer routed to DOWN nodes
         * and the consistent hash ring is rebuilt to exclude them.
         * Default: 20000ms (4 missed heartbeat cycles).
         */
        @Min(value = 1000, message = "Down threshold must be at least 1000ms")
        private long downThresholdMs = 20000;

        /**
         * Static list of peer node addresses in "host:port" format.
         * This node will send heartbeat pings to each address in this list.
         * Used for peer-to-peer liveness monitoring.
         * Example: ["localhost:8082", "localhost:8083"]
         */
        private java.util.List<String> peers = new java.util.ArrayList<>();

        public int getConnectTimeoutMs() { return connectTimeoutMs; }
        public void setConnectTimeoutMs(int connectTimeoutMs) { this.connectTimeoutMs = connectTimeoutMs; }

        public int getReadTimeoutMs() { return readTimeoutMs; }
        public void setReadTimeoutMs(int readTimeoutMs) { this.readTimeoutMs = readTimeoutMs; }

        public int getMaxRetries() { return maxRetries; }
        public void setMaxRetries(int maxRetries) { this.maxRetries = maxRetries; }

        public int getVirtualNodesPerNode() { return virtualNodesPerNode; }
        public void setVirtualNodesPerNode(int virtualNodesPerNode) { this.virtualNodesPerNode = virtualNodesPerNode; }

        public int getReplicationFactor() { return replicationFactor; }
        public void setReplicationFactor(int replicationFactor) { this.replicationFactor = replicationFactor; }

        public long getHeartbeatIntervalMs() { return heartbeatIntervalMs; }
        public void setHeartbeatIntervalMs(long heartbeatIntervalMs) { this.heartbeatIntervalMs = heartbeatIntervalMs; }

        public long getHeartbeatTimeoutMs() { return heartbeatTimeoutMs; }
        public void setHeartbeatTimeoutMs(long heartbeatTimeoutMs) { this.heartbeatTimeoutMs = heartbeatTimeoutMs; }

        public long getSuspectThresholdMs() { return suspectThresholdMs; }
        public void setSuspectThresholdMs(long suspectThresholdMs) { this.suspectThresholdMs = suspectThresholdMs; }

        public long getDownThresholdMs() { return downThresholdMs; }
        public void setDownThresholdMs(long downThresholdMs) { this.downThresholdMs = downThresholdMs; }

        public java.util.List<String> getPeers() { return peers; }
        public void setPeers(java.util.List<String> peers) { this.peers = peers; }
    }

    // Replication configuration

    public static class ReplicationProperties {

        /**
         * Whether replication runs asynchronously (non-blocking) or synchronously.
         * <p>Async (default): the client receives a response as soon as the primary write
         * succeeds; replicas are written in the background on a dedicated thread pool.
         * <p>Sync: the client waits until at least {@code minAckNodes} replicas acknowledge
         * before receiving a response. Higher durability but higher latency.
         * Default: true (async).
         */
        private boolean async = true;

        /**
         * Minimum number of replica nodes that must acknowledge a write before the
         * coordinator considers it complete. Only relevant when {@code async=false}.
         * Default: 1 (at least one replica must ACK in sync mode).
         */
        @Min(value = 0, message = "minAckNodes must be >= 0")
        private int minAckNodes = 1;

        /**
         * Number of threads in the dedicated replication executor thread pool.
         * Each thread handles one replication call to one replica node.
         * Too few threads = replication queue builds up under high write load.
         * Too many threads = thread contention on small clusters.
         * Default: 4 threads — suitable for clusters of up to ~8 nodes.
         */
        @Min(value = 1, message = "threadPoolSize must be at least 1")
        private int threadPoolSize = 4;

        public boolean isAsync() { return async; }
        public void setAsync(boolean async) { this.async = async; }

        public int getMinAckNodes() { return minAckNodes; }
        public void setMinAckNodes(int minAckNodes) { this.minAckNodes = minAckNodes; }

        public int getThreadPoolSize() { return threadPoolSize; }
        public void setThreadPoolSize(int threadPoolSize) { this.threadPoolSize = threadPoolSize; }
    }
}
