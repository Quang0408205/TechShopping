package com.example.Tech.dto.request.auth;

import jakarta.validation.constraints.NotBlank;

/**
 * Used by both /auth/refresh and /auth/logout.
 */
public record RefreshTokenRequest(

        @NotBlank(message = "Refresh token is required")
        String refreshToken
) {
}
