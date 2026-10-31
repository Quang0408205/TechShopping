package com.example.Tech.dto.request.auth;

import com.example.Tech.util.AccountUtil;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(

        @NotBlank(message = "Token is required")
        @Size(max = 200, message = "Token is too long")
        String token,

        @NotBlank(message = "Password is required")
        @Size(min = AccountUtil.MIN_PASSWORD_LENGTH, max = AccountUtil.MAX_PASSWORD_BYTES,
                message = "Password must be 8 to 72 characters")
        String newPassword
) {
}
