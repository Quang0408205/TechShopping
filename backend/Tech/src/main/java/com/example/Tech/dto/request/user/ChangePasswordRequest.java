package com.example.Tech.dto.request.user;

import com.example.Tech.util.AccountUtil;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(

        @NotBlank(message = "Current password is required")
        String currentPassword,

        @NotBlank(message = "New password is required")
        @Size(min = AccountUtil.MIN_PASSWORD_LENGTH, max = AccountUtil.MAX_PASSWORD_BYTES,
                message = "New password must be 8 to 72 characters")
        String newPassword
) {
}
