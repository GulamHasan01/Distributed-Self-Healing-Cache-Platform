package com.cache.cluster.routing;

import com.cache.cluster.model.NodeInfo;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * An immutable consistent hash ring that maps cache keys to node IDs.
 *
 * <p><strong>How it works</strong></p>
 * <ol>
 *   <li>Each physical node is placed at {@code virtualNodesPerNode} positions on the ring.
 *       Position = MD5_to_long(nodeId + "#" + vnodeIndex).</li>
 *   <li>A key is hashed to a position; the ring is walked clockwise to find the
 *       nearest entry &gt;= that position (wrapping to the start if needed).</li>
 *   <li>Adding/removing a node only moves ~(1/N) of keys to the new owner.</li>
 * </ol>
 *
 * <p><strong>Why MD5?</strong> Used purely for uniform distribution across 2^64 space.
 * Security properties are irrelevant here. MD5 is guaranteed by the Java spec.</p>
 *
 * <p><strong>Thread safety:</strong> IMMUTABLE after construction.
 * Create a new ring whenever cluster membership changes.</p>
 *
 * <p><strong>Phase 6 note:</strong> A cached ring rebuilt only on cluster-change events
 * will replace the current per-request rebuild approach.</p>
 */
public class ConsistentHashRing {

    private final TreeMap<Long, String> ring;
    private final int physicalNodeCount;

    /**
     * Builds a consistent hash ring from the given nodes.
     *
     * @param nodes               nodes to place on the ring (typically UP nodes only)
     * @param virtualNodesPerNode virtual positions per physical node (recommended: 150)
     */
    public ConsistentHashRing(Collection<NodeInfo> nodes, int virtualNodesPerNode) {
        this.ring = new TreeMap<>();
        for (NodeInfo node : nodes) {
            for (int i = 0; i < virtualNodesPerNode; i++) {
                long position = hash(node.getId() + "#" + i);
                ring.put(position, node.getId());
            }
        }
        this.physicalNodeCount = nodes.size();
    }

    /**
     * Returns the node ID that owns the given cache key.
     *
     * <p>Finds the first ring position &gt;= hash(key), wrapping to the first position
     * when the hash exceeds all existing positions.</p>
     *
     * @param key the cache key
     * @return the responsible node ID, or {@code Optional.empty()} if the ring is empty
     */
    public Optional<String> getNodeForKey(String key) {
        if (ring.isEmpty()) {
            return Optional.empty();
        }
        long position = hash(key);
        Map.Entry<Long, String> entry = ring.ceilingEntry(position);
        if (entry == null) {
            entry = ring.firstEntry(); // wrap around
        }
        return Optional.of(entry.getValue());
    }

    /**
     * Returns up to {@code count} unique node IDs clockwise starting from the
     * hash position of the given key.
     *
     * <p>If there are fewer physical nodes in the ring than {@code count},
     * all physical nodes represented in the ring are returned.</p>
     *
     * @param key   the cache key
     * @param count maximum number of unique nodes to retrieve
     * @return a list of unique node IDs, starting with the primary owner
     */
    public List<String> getNodesForKey(String key, int count) {
        if (ring.isEmpty() || count <= 0) {
            return new ArrayList<>();
        }
        List<String> result = new ArrayList<>();
        long position = hash(key);

        Map.Entry<Long, String> entry = ring.ceilingEntry(position);
        if (entry == null) {
            entry = ring.firstEntry();
        }

        while (result.size() < count && result.size() < physicalNodeCount) {
            String nodeId = entry.getValue();
            if (!result.contains(nodeId)) {
                result.add(nodeId);
            }

            Long nextKey = ring.higherKey(entry.getKey());
            if (nextKey == null) {
                entry = ring.firstEntry();
            } else {
                entry = ring.ceilingEntry(nextKey);
            }
        }
        return result;
    }

    /**
     * Returns the number of physical nodes represented in this ring.
     */
    public int size() {
        return physicalNodeCount;
    }

    /**
     * Returns the underlying ring mapping of virtual node hashes to physical node IDs.
     */
    public java.util.Map<Long, String> getRingMap() {
        return java.util.Collections.unmodifiableMap(ring);
    }

    private static final ThreadLocal<MessageDigest> MD5_DIGEST = ThreadLocal.withInitial(() -> {
        try {
            return MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 algorithm unexpectedly unavailable", e);
        }
    });

    /**
     * Hashes a string to a long using MD5 (first 8 bytes, big-endian).
     */
    public static long hash(String input) {
        MessageDigest md = MD5_DIGEST.get();
        md.reset();
        byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
        long result = 0;
        for (int i = 0; i < 8; i++) {
            result = (result << 8) | (digest[i] & 0xFFL);
        }
        return result;
    }
}