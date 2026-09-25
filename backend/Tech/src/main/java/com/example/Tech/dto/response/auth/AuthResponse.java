package com.example.Tech.dto.response.auth;

import io.swagger.v3.oas.annotations.media.Schema;

public record AuthResponse(

        @Schema(description = "JWT to send as 'Authorization: Bearer <token>'")
        String accessToken,

        @Schema(description = "Single-use token for /auth/refresh and /auth/logout")
        String refreshToken,

        @Schema(example = "Bearer")
        String tokenType,

        @Schema(description = "Access token lifetime in seconds", example = "1800")
        long expiresIn,

        AuthUserResponse user
) {

    public static final String BEARER = "Bearer";
}
