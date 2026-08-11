package com.cache.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * Unified API response wrapper for ALL endpoints in this service.
 *
 * <p>WHY a response wrapper?</p>
 * Without a wrapper, each endpoint returns different shapes:
 * - GET /cache/key returns the value directly
 * - POST /cache returns nothing or the key
 * - Errors return a Spring default error object
 *
 * This is a nightmare for API consumers. A wrapper enforces a contract:
 * EVERY response always has: success, message, timestamp, and optionally data/error.
 * The client always knows what field to check.
 *
 * <p>WHY a generic {@code <T>} data field?</p>
 * The same wrapper works for single entries, lists, stats, anything.
 * The type system keeps it safe — no casting.
 *
 * <p>@JsonInclude(NON_NULL) — null fields are omitted from the JSON response.
 * This keeps responses clean: when there's no error, "error" field disappears entirely.</p>
 *
 * <p>WHY a record?</p>
 * ApiResponse is produced and never mutated. Records are perfect for this.
 *
 * Example success response:
 * <pre>
 * {
 *   "success": true,
 *   "message": "Key stored successfully",
 *   "timestamp": "2024-01-15T10:30:00Z",
 *   "data": { "key": "user:1001", "value": "{...}" }
 * }
 * </pre>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Standard API response envelope used by all endpoints")
public record ApiResponse<T>(

        @Schema(description = "true if the operation succeeded, false otherwise")
        boolean success,

        @Schema(description = "Human-readable message describing the result")
        String message,

        @Schema(description = "UTC timestamp of the response")
        Instant timestamp,

        @Schema(description = "Response payload; absent on error responses")
        T data,

        @Schema(description = "Error detail; absent on success responses")
        String error

) {

    // -------------------------------------------------------------------------
    // Static factory methods — more readable than calling the constructor
    // -------------------------------------------------------------------------

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, Instant.now(), data, null);
    }

    public static <T> ApiResponse<T> success(String message) {
        return new ApiResponse<>(true, message, Instant.now(), null, null);
    }

    public static <T> ApiResponse<T> failure(String message, String error) {
        return new ApiResponse<>(false, message, Instant.now(), null, error);
    }
}
