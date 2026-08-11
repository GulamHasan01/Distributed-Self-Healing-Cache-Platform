package com.cache.scheduler;

import com.cache.store.CacheStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Background scheduler responsible for proactively removing expired cache entries.
 *
 * <p>WHY a separate scheduler class?</p>
 * <ul>
 *   <li>Single Responsibility: The scheduler's only job is to trigger expiry sweeps.
 *       It doesn't know HOW expiry works — that's the store's responsibility.</li>
 *   <li>Independently testable: We can test the scheduler with a mock store.</li>
 *   <li>The timing logic lives here; the expiry logic lives in the store.
 *       These concerns are separated.</li>
 * </ul>
 *
 * <p>Why is @EnableScheduling on the main application class and not here?</p>
 * @EnableScheduling is a module-level toggle. If it were here, adding another
 * scheduler class later would require another @EnableScheduling — confusing and
 * redundant. Keeping it at the application root makes it a single global setting.
 *
 * <p>How @Scheduled works:</p>
 * Spring creates a dedicated thread pool for scheduled tasks (default: 1 thread).
 * {@code fixedDelay} means: wait N ms AFTER the previous execution completes before
 * starting the next. This prevents overlap — if a sweep takes 3 seconds and
 * fixedDelay is 5 seconds, the next sweep starts 5 seconds after the 3-second sweep
 * finishes (total 8 seconds apart), NOT 5 seconds from start.
 *
 * <p>Compare: {@code fixedRate} starts execution every N ms from the START of
 * the previous execution. If sweeps are slow, fixedRate can cause overlapping
 * executions. fixedDelay is safer for cleanup tasks.</p>
 *
 * <p>The interval comes from config: {@code cache.ttl.sweep-interval-ms}.
 * Using SpEL ({@code ${...}}) makes the interval externally configurable.
 * The default of 5000 is the fallback if the property is missing.</p>
 */
@Component
public class CacheExpirationScheduler {

    private static final Logger log = LoggerFactory.getLogger(CacheExpirationScheduler.class);

    private final CacheStore cacheStore;

    public CacheExpirationScheduler(CacheStore cacheStore) {
        this.cacheStore = cacheStore;
    }

    /**
     * Sweeps the cache store for expired entries on a fixed delay.
     *
     * <p>The sweep is logged at DEBUG level normally to avoid polluting logs.
     * Only when entries are actually removed is it promoted to INFO.</p>
     */
    @Scheduled(fixedDelayString = "${cache.ttl.sweep-interval-ms:5000}")
    public void sweepExpiredEntries() {
        log.debug("TTL sweep starting — store size: {}", cacheStore.size());

        int removed = cacheStore.removeExpired();

        if (removed > 0) {
            log.info("TTL sweep complete: removed {} expired entries. Store size now: {}",
                    removed, cacheStore.size());
        } else {
            log.debug("TTL sweep complete: no expired entries found");
        }
    }
}
