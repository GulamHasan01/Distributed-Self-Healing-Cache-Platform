package com.community.iam_service.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

/**
 * Personal API keys for programmatic platform access.
 * Developers can create keys with specific scopes (read:projects, write:projects, etc.)
 * Keys are shown ONCE on creation — only the hash is stored.
 */
@Document(collection = "api_keys")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiKey {

    @Id
    private String id;

    private String userId;

    private String name;            // e.g. "My CI/CD Key", "Local Dev"

    @Indexed(unique = true)
    private String keyHash;         // SHA-256 hash of the raw key — never store raw

    private String keyPrefix;       // first 8 chars for identification (shown to user)

    private List<String> scopes;    // e.g. ["read:projects", "write:projects"]

    @Builder.Default
    private boolean active = true;

    private Instant expiresAt;      // null = never expires

    private Instant lastUsedAt;

    private Instant createdAt;
}