package com.cache;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point for the Cache Service.
 *
 * <p>@ConfigurationPropertiesScan tells Spring to auto-discover all
 * @ConfigurationProperties beans in this package and its sub-packages.
 * This is the modern alternative to @EnableConfigurationProperties.</p>
 *
 * <p>@EnableScheduling is added now so Phase 2 TTL expiration scheduler
 * compiles without any application-level change.</p>
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public class CacheServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CacheServiceApplication.class, args);
    }
}
