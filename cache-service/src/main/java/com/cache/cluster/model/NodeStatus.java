package com.cache.cluster.model;

/**
 * Lifecycle status of a cache node in the cluster.
 *
 * <p>State transitions:</p>
 * <pre>
 *   STARTING ──► UP ──► SUSPECT ──► DOWN
 *                ▲_______/
 * </pre>
 *
 * <ul>
 *   <li>STARTING: Node has registered itself but has not yet signalled readiness.
 *       It should NOT receive traffic during this state.</li>
 *   <li>UP: Node is healthy and serving requests.</li>
 *   <li>SUSPECT: Node has missed one or more heartbeat windows. May be overloaded
 *       or experiencing a partial network partition. Used in Phase 7.</li>
 *   <li>DOWN: Confirmed dead — multiple missed heartbeats. No traffic should be
 *       routed to this node. Used in Phase 8 for failover.</li>
 * </ul>
 *
 * <p>WHY an enum and not a String?</p>
 * Enums are type-safe and eliminate entire categories of bugs:
 * "up" vs "UP" vs "Up" — all different strings but represent the same intent.
 * An enum fails at compile time if you misspell it.
 */
public enum NodeStatus {

    /** Node has registered but is not yet ready to serve traffic. */
    STARTING,

    /** Node is healthy and serving requests normally. */
    UP,

    /** Node has missed heartbeat(s) — under observation. (Phase 7) */
    SUSPECT,

    /** Node is confirmed dead — removed from routing. (Phase 8) */
    DOWN
}
