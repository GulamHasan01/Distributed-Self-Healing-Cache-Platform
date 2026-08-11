package com.cache.cluster.exception;

/**
 * Thrown when inter-node HTTP communication fails.
 *
 * <p>Wraps lower-level exceptions (network errors, timeouts, unexpected HTTP status)
 * into a single, consistent exception type that callers can handle uniformly.</p>
 *
 * <p>Mapped to HTTP 502 Bad Gateway by the GlobalExceptionHandler.
 * The semantics: "I (the proxy) received an invalid response from an upstream server
 * (the remote cache node)." — this is precisely what RFC 7231 defines 502 for.</p>
 *
 * <p>WHY 502 and not 503?</p>
 * <ul>
 *   <li>502 Bad Gateway = The upstream server sent an invalid response (wrong format, error, etc.)</li>
 *   <li>503 Service Unavailable = THIS server is not ready to serve requests</li>
 * </ul>
 * When a remote cache node returns an error, this node is still up — the remote is not.
 * 502 is the correct choice.
 */
public class NodeCommunicationException extends RuntimeException {

    private final String targetNodeId;
    private final String targetUrl;

    public NodeCommunicationException(String targetNodeId, String targetUrl, String message) {
        super(String.format("Communication failure with node '%s' at '%s': %s",
                targetNodeId, targetUrl, message));
        this.targetNodeId = targetNodeId;
        this.targetUrl = targetUrl;
    }

    public NodeCommunicationException(String targetNodeId, String targetUrl,
                                      String message, Throwable cause) {
        super(String.format("Communication failure with node '%s' at '%s': %s",
                targetNodeId, targetUrl, message), cause);
        this.targetNodeId = targetNodeId;
        this.targetUrl = targetUrl;
    }

    public String getTargetNodeId() { return targetNodeId; }
    public String getTargetUrl() { return targetUrl; }
}
