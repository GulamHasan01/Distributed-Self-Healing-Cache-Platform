package com.cache.config;

import com.cache.eviction.EvictionPolicy;
import com.cache.eviction.LRUEvictionPolicy;
import com.cache.eviction.NoEvictionPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration that instantiates the correct EvictionPolicy bean
 * based on the {@code cache.eviction-policy} property.
 *
 * <p>WHY a dedicated @Configuration class?</p>
 * The eviction policy depends on a configuration value known only at runtime.
 * We can't use @ConditionalOnProperty easily for this pattern since we need
 * to pass the actual CacheProperties enum value. A @Bean factory method
 * reads the configuration and returns the correct implementation —
 * this is the Factory Method pattern applied to Spring bean creation.
 *
 * <p>Result: {@code InMemoryCacheStore} gets the correct {@code EvictionPolicy}
 * injected by Spring without knowing anything about the configuration.
 * The selection logic lives here — in one place — not scattered across
 * the store or service.</p>
 */
@Configuration
public class EvictionPolicyConfig {

    private static final Logger log = LoggerFactory.getLogger(EvictionPolicyConfig.class);

    @Bean
    public EvictionPolicy evictionPolicy(CacheProperties properties) {
        EvictionPolicy policy = switch (properties.getEvictionPolicy()) {
            case LRU -> new LRUEvictionPolicy();
            case NO_EVICTION -> new NoEvictionPolicy();
        };

        log.info("Eviction policy configured: {}", policy.policyName());
        return policy;
    }
}
