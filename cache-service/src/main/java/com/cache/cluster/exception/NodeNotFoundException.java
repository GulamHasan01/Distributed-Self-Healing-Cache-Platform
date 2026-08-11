package com.cache.cluster.exception;

/**
 * Thrown when a requested node is not found in the cluster registry.
 *
 * <p>Mapped to HTTP 404 Not Found by the GlobalExceptionHandler.</p>
 */
public class NodeNotFoundException extends RuntimeException {

    private final String nodeId;

    public NodeNotFoundException(String nodeId) {
        super("Node not found in cluster registry: id='" + nodeId + "'");
        this.nodeId = nodeId;
    }

    public String getNodeId() {
        return nodeId;
    }
}
