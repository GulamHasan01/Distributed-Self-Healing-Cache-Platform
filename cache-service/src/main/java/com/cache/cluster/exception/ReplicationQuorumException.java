package com.cache.cluster.exception;

/**
 * Thrown when synchronous replication fails to receive the minimum required
 * replica acknowledgments (acks) specified by {@code minAckNodes}.
 */
public class ReplicationQuorumException extends RuntimeException {

    public ReplicationQuorumException(String message) {
        super(message);
    }
}
