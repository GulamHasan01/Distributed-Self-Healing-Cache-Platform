package com.community.iam_service.dtos.response;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

// ── ApiKey response — raw key shown ONCE on creation only ────────────────────
@Data
@Builder
public class ApiKeyResponse {
    private String id;
    private String name;
    private String keyPrefix;       // e.g. "a1b2c3d4" — for identification
    private String rawKey;          // ⚠️ only populated on creation, null after
    private List<String> scopes;
    private boolean active;
    private Instant expiresAt;
    private Instant lastUsedAt;
    private Instant createdAt;
}