package com.community.iam_service.dtos.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ✅ FIXED: was only returning accessToken.
 * Now includes refreshToken so the client can use it to get new access tokens.
 *
 * Note: In a high-security setup, deliver refreshToken via HttpOnly cookie instead
 * of JSON body to protect against XSS. See AuthController for cookie-based approach.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL) // don't serialize null fields
public class AuthResponse {
    private String accessToken;
    private String refreshToken; // ✅ NEW: returned on login/register
    private String tokenType = "Bearer";
}
