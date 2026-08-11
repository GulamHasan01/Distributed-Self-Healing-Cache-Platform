package com.cache.cluster.health;

/**
 * Contract for the failure detector — the component that runs periodic sweeps
 * of the cluster registry to transition stale nodes through the
 * {@code UP → SUSPECT → DOWN} lifecycle.
 *
 * <p><strong>Why a separate failure detector from the heartbeat emitter?</strong></p>
 * Single Responsibility Principle:
 * <ul>
 *   <li>The <em>heartbeat emitter</em> produces data — it pings peers and records
 *       whether they responded.</li>
 *   <li>The <em>failure detector</em> consumes data — it reads {@code lastHeartbeatAt}
 *       timestamps and makes state-transition decisions.</li>
 * </ul>
 * Keeping them separate makes each independently testable and replaceable.
 * For example, you could swap the heartbeat strategy (HTTP → gossip) without touching
 * the failure detection thresholds, and vice versa.
 *
 * <p>Implementation is scheduled via {@code @Scheduled} at half the
 * {@code suspectThresholdMs} interval so nodes are detected promptly.</p>
 */
public interface FailureDetectorService {

    /**
     * Run one full detection cycle across all registered nodes.
     *
     * <p>For each non-self node in the cluster registry:</p>
     * <ol>
     *   <li>Compute age = {@code now - lastHeartbeatAt}</li>
     *   <li>If {@code age > downThresholdMs} and status is not already DOWN →
     *       transition to DOWN and trigger ring rebuild.</li>
     *   <li>If {@code age > suspectThresholdMs} and status is UP →
     *       transition to SUSPECT and trigger ring rebuild.</li>
     * </ol>
     *
     * <p>Called on a schedule — do not call directly in production code.</p>
     */
    void runDetectionCycle();
}
