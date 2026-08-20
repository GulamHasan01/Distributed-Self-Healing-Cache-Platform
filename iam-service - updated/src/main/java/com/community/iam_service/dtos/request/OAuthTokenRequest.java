package com.community.iam_service.dtos.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class OAuthTokenRequest {

    // The ID token received from Google or LinkedIn on the frontend
    @NotBlank(message = "Token is required")
    private String idToken;
}
