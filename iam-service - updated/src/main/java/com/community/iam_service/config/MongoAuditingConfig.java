package com.community.iam_service.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

/**
 * ✅ NEW: Enables Spring Data MongoDB auditing.
 * Required for @CreatedDate and @LastModifiedDate to work on entity fields.
 * Without this annotation, those fields are never populated automatically.
 */
@Configuration
@EnableMongoAuditing
public class MongoAuditingConfig {
    // No additional configuration needed — annotation does all the work.
}
