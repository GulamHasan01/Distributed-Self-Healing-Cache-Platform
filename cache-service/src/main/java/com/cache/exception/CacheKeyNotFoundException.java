package com.cache.exception;

/**
 * Thrown when a GET or DELETE is attempted for a key that does not exist
 * in the cache store.
 *
 * <p>WHY a custom exception?</p>
 * Using a generic RuntimeException would force the GlobalExceptionHandler to
 * inspect exception messages (fragile, string-matching). A dedicated exception
 * type is handled by its class — robust and refactor-safe.
 *
 * <p>This is an unchecked exception (extends RuntimeException) because:
 * - Cache misses are expected in normal operation — not exceptional circumstances
 *   that callers must handle at compile time
 * - Spring's @ExceptionHandler only works with unchecked exceptions in most flows
 * - Modern Java advice (Effective Java Item 71) recommends checked exceptions only
 *   for conditions callers can reasonably recover from in a meaningful way</p>
 */
public class CacheKeyNotFoundException extends RuntimeException {

    private final String key;

    public CacheKeyNotFoundException(String key) {
        super("Cache key not found: '" + key + "'");
        this.key = key;
    }

    public String getKey() {
        return key;
    }
}
