package com.cache.cluster.replication;

/**
 * Immutable snapshot of the current replication health metrics.
 *
 * <p>Returned by {@link ReplicationService#getStats()} and included in the
 * {@code GET /api/v1/cache/stats} response so operators can see, at a glance,
 * how many replication calls have succeeded or failed since startup.</p>
 *
 * @param totalAttempted    Total number of individual replica replication calls attempted since startup.
 * @param succeeded         Calls that received an HTTP 2xx from the target replica.
 * @param failed            Calls that timed out, threw an exception, or received a non-2xx.
 * @param successRatePercent Success rate as a percentage rounded to 1 decimal place.
 *                           Returns 100.0 when totalAttempted == 0 (no failures yet).
 */
public record ReplicationStats(
        long totalAttempted,
        long succeeded,
        long failed,
        double successRatePercent
) {

    /** Convenience factory — computes successRatePercent from raw counters. */
    public static ReplicationStats of(long totalAttempted, long succeeded, long failed) {
        double rate = totalAttempted == 0
                ? 100.0
                : Math.round((succeeded * 100.0 / totalAttempted) * 10.0) / 10.0;
        return new ReplicationStats(totalAttempted, succeeded, failed, rate);
    }

    /** Returns a zero-valued snapshot (used before any replication has occurred). */
    public static ReplicationStats empty() {
        return new ReplicationStats(0L, 0L, 0L, 100.0);
    }
}
