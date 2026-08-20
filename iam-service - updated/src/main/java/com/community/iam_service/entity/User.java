package com.community.iam_service.entity;

import com.community.iam_service.entity.Enum.AuthProvider;
import com.community.iam_service.entity.Enum.Role;
import com.community.iam_service.entity.Enum.UserStatus;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Document(collection = "users")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class User {

    @Id
    private String id;

    private String name;

    @Indexed(unique = true)
    @NotBlank
    private String email;

    // NEW: unique username for public profile URLs like platform.com/@username
    @Indexed(unique = true, sparse = true)
    private String username;

    private String password;

    @Builder.Default
    private List<Role> roles = List.of(Role.USER);

    @Builder.Default
    private UserStatus status = UserStatus.ACTIVE;

    @Builder.Default
    private AuthProvider provider = AuthProvider.LOCAL;

    @Builder.Default
    private boolean emailVerified = false;

    // NEW: creator verification (like Twitter verified — admin grants it)
    @Builder.Default
    private boolean verified = false;

    // NEW: referral tracking — who invited this user
    private String referredBy; // userId of referrer

    // NEW: account lockout after failed logins (brute force protection)
    @Builder.Default
    private int failedLoginAttempts = 0;
    private Instant lockedUntil; // null means not locked

    // NEW: deactivation (different from DELETED — user can reactivate)
    @Builder.Default
    private boolean deactivated = false;
    private Instant deactivatedAt;

    // NEW: profile completion score (0-100) — shown on dashboard
    @Builder.Default
    private int profileCompletionScore = 0;

    // NEW: last known IP for suspicious login detection
    private String lastLoginIp;

    private Instant lastLoginAt;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    private boolean enabled;
}