package com.cache.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 3.0 documentation configuration.
 *
 * <p>Accessible at: http://localhost:8081/swagger-ui.html</p>
 * <p>Raw spec at:   http://localhost:8081/api-docs</p>
 *
 * <p>WHY OpenAPI?</p>
 * In a microservice world, every service is a black box to other teams.
 * A machine-readable API contract allows:
 * - Auto-generated client SDKs
 * - Contract-first testing
 * - API Gateway routing configuration
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI cacheServiceOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Cache Service API")
                        .description("""
                                Phase 1 — Single Node In-Memory Cache.
                                Provides PUT, GET, DELETE, CLEAR, and STATS operations
                                backed by a thread-safe ConcurrentHashMap.
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Cache Platform Team")
                                .email("cache-team@platform.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")));
    }
}
