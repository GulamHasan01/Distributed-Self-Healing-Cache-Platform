package com.cache.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Spring configuration for the Phase 8 async replication executor.
 *
 * <p>WHY a dedicated thread pool?</p>
 * <p>Replication calls are outbound HTTP requests to peer nodes. They can block
 * on I/O for up to {@code readTimeoutMs} milliseconds. Sharing the default Spring
 * scheduler or the HTTP request thread pool for this work would starve real
 * client requests under load. A dedicated pool isolates replication I/O
 * from the primary request path.</p>
 *
 * <p>Rejection policy: {@link CallerRunsPolicy}.</p>
 * <p>If the replication queue fills up (e.g., a burst of writes with slow replicas),
 * the calling thread (the request thread) executes the replication directly.
 * This provides natural back-pressure: the client slows down instead of losing
 * replication tasks silently.</p>
 */
@Configuration
public class ReplicationConfig {

    private final CacheProperties cacheProperties;

    public ReplicationConfig(CacheProperties cacheProperties) {
        this.cacheProperties = cacheProperties;
    }

    /**
     * Thread pool executor used exclusively for async replica write/delete calls.
     *
     * <ul>
     *   <li>Core / Max pool size: {@code cache.replication.thread-pool-size} (default 4)</li>
     *   <li>Queue capacity: 500 pending tasks</li>
     *   <li>Thread name prefix: {@code replication-}</li>
     *   <li>Await termination: 30 s on application shutdown to flush in-flight tasks</li>
     * </ul>
     */
    @Bean(name = "replicationExecutor")
    public Executor replicationExecutor() {
        int poolSize = cacheProperties.getReplication().getThreadPoolSize();

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(poolSize);
        executor.setMaxPoolSize(poolSize * 2);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("replication-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}
