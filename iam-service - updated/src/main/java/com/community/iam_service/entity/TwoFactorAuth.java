package com.community.iam_service.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * Stores TOTP 2FA secrets per user.
 * Flow:
 *  1. User enables 2FA → secret generated → QR code shown
 *  2. User scans with Google Authenticator and confirms with a code
 *  3. enabled = true
 *  4. On login → if enabled, ask for TOTP code before issuing JWT
 */
@Document(collection = "two_factor_auth")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TwoFactorAuth {

    @Id
    private String id;

    @Indexed(unique = true)
    private String userId;

    private String secret;          // Base32-encoded TOTP secret

    @Builder.Default
    private boolean enabled = false; // false until user confirms setup

    private Instant enabledAt;
    private Instant createdAt;
}