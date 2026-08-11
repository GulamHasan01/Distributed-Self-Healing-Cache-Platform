package com.cache.exception;

import com.cache.dto.response.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import com.cache.cluster.exception.NodeNotFoundException;
import com.cache.cluster.exception.NodeAlreadyExistsException;
import com.cache.cluster.exception.NodeCommunicationException;

import java.util.stream.Collectors;

/**
 * Centralized exception handler for the entire service.
 *
 * <p>WHY @RestControllerAdvice?</p>
 * Without this class, every controller would need its own try-catch blocks
 * which is repetitive, inconsistent, and hard to maintain. @RestControllerAdvice
 * intercepts exceptions thrown anywhere in the controller layer and maps them
 * to standardized HTTP responses — all in one place. This is the Open/Closed
 * Principle applied to exception handling: you can add new exceptions without
 * modifying existing handlers.
 *
 * <p>WHY @RestControllerAdvice not @ControllerAdvice?</p>
 * @RestControllerAdvice = @ControllerAdvice + @ResponseBody.
 * Since we always return JSON, we use the REST variant.
 *
 * <p>Design rule: EVERY exception must be logged at the appropriate level:
 * - 4xx (client errors) → WARN (client did something wrong, not our fault)
 * - 5xx (server errors) → ERROR (our fault, needs investigation)
 * Never log 4xx as ERROR — it pollutes on-call alerts.</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // -------------------------------------------------------------------------
    // Domain-specific exceptions
    // -------------------------------------------------------------------------

    /**
     * Cache miss — key not found.
     * HTTP 404 Not Found.
     */
    @ExceptionHandler(CacheKeyNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleKeyNotFound(CacheKeyNotFoundException ex) {
        log.warn("Cache miss: key='{}' not found", ex.getKey());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.failure("Key not found", ex.getMessage()));
    }

    /**
     * Cache full with no eviction policy.
     * HTTP 507 Insufficient Storage — the semantically correct status for a full store.
     */
    @ExceptionHandler(CacheCapacityExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleCapacityExceeded(CacheCapacityExceededException ex) {
        log.warn("Cache capacity exceeded: max={}, current={}", ex.getMaxCapacity(), ex.getCurrentSize());
        return ResponseEntity
                .status(HttpStatus.INSUFFICIENT_STORAGE)
                .body(ApiResponse.failure("Cache is full", ex.getMessage()));
    }

    // -------------------------------------------------------------------------
    // Cluster node exceptions
    // -------------------------------------------------------------------------

    /**
     * Node not found in cluster registry.
     * HTTP 404 Not Found.
     */
    @ExceptionHandler(NodeNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNodeNotFound(NodeNotFoundException ex) {
        log.warn("Cluster: node not found: id='{}'", ex.getNodeId());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.failure("Node not found", ex.getMessage()));
    }

    /**
     * Duplicate node registration conflict.
     * HTTP 409 Conflict.
     */
    @ExceptionHandler(NodeAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<Void>> handleNodeAlreadyExists(NodeAlreadyExistsException ex) {
        log.warn("Cluster: duplicate node registration: id='{}'", ex.getNodeId());
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.failure("Node already registered", ex.getMessage()));
    }

    /**
     * Node communication failure.
     * HTTP 502 Bad Gateway.
     */
    @ExceptionHandler(NodeCommunicationException.class)
    public ResponseEntity<ApiResponse<Void>> handleNodeCommunication(NodeCommunicationException ex) {
        log.error("Cluster: communication error with node '{}' at '{}': {}",
                ex.getTargetNodeId(), ex.getTargetUrl(), ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_GATEWAY)
                .body(ApiResponse.failure("Bad Gateway", ex.getMessage()));
    }
    @ExceptionHandler(com.cache.cluster.exception.ReplicationQuorumException.class)
    public ResponseEntity<ApiResponse<Void>> handleReplicationQuorum(com.cache.cluster.exception.ReplicationQuorumException ex) {
        log.error("Cluster: replication quorum failed: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.GATEWAY_TIMEOUT)
                .body(ApiResponse.failure("Gateway Timeout", ex.getMessage()));
    }

    // -------------------------------------------------------------------------
    // Validation exceptions
    // -------------------------------------------------------------------------

    /**
     * Bean validation failure — @NotBlank, @Size, etc.
     * HTTP 400 Bad Request.
     *
     * <p>Collects ALL field errors into a single readable message instead of
     * returning only the first error. Better DX for API consumers.</p>
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        String errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));

        log.warn("Validation failed: {}", errors);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.failure("Validation failed", errors));
    }

    /**
     * Path variable type mismatch (e.g., passing a string where a Long is expected).
     * HTTP 400 Bad Request.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String error = String.format("Parameter '%s' has invalid value '%s'", ex.getName(), ex.getValue());
        log.warn("Type mismatch: {}", error);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.failure("Invalid parameter", error));
    }

    // -------------------------------------------------------------------------
    // Fallback & client reading exceptions
    // -------------------------------------------------------------------------

    /**
     * Missing or malformed HTTP request body.
     * HTTP 400 Bad Request.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleMessageNotReadable(HttpMessageNotReadableException ex) {
        log.warn("Request body is missing or malformed: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.failure("Malformed request body", "Required request body is missing or invalid"));
    }

    // -------------------------------------------------------------------------
    // Catch-all fallback
    // -------------------------------------------------------------------------

    /**
     * Unhandled exceptions — logged as ERROR since they indicate a bug.
     * HTTP 500 Internal Server Error.
     *
     * <p>The error message exposed to the client is intentionally generic.
     * We NEVER expose stack traces or internal details to API consumers —
     * this is a security requirement.</p>
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneric(Exception ex) {
        log.error("Unhandled exception: {}", ex.getMessage(), ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.failure(
                        "An unexpected error occurred",
                        "Please contact support if the problem persists"
                ));
    }
}
