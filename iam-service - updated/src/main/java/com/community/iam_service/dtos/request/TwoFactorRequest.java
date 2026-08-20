package com.community.iam_service.dtos.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

public class TwoFactorRequest {

    @Data
    public static class VerifySetup {
        @NotBlank
        @Size(min = 6, max = 6, message = "TOTP code must be 6 digits")
        private String code; // confirm 2FA setup with first code
    }

    @Data
    public static class Verify {
        @NotBlank
        @Size(min = 6, max = 6, message = "TOTP code must be 6 digits")
        private String code; // verify on login
    }

    @Data
    public static class Disable {
        @NotBlank
        private String password; // require current password to disable 2FA
    }
}