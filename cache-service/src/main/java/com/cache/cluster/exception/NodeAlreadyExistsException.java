package com.cache.cluster.exception;

/**
 * Thrown when trying to register a node with an ID that already exists
 * in the registry (and re-registration is explicitly disallowed).
 *
 * <p>Note: The default registration behavior is IDEMPOTENT — re-registering
 * the same node ID replaces the existing entry. This exception is reserved
 * for scenarios where duplicate registration must be treated as an error.</p>
 *
 * <p>Mapped to HTTP 409 Conflict by the GlobalExceptionHandler.</p>
 */
public class NodeAlreadyExistsException extends RuntimeException {

    private final String nodeId;

    public NodeAlreadyExistsException(String nodeId) {
        super("Node with id='" + nodeId + "' is already registered in the cluster");
        this.nodeId = nodeId;
    }

    public String getNodeId() {
        return nodeId;
    }
}
