package com.example.Tech.dto.response.user;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/** The new administrator plus the temporary password: shown only in this response, stored only as a hash. */
public record AdminCreatedResponse(
        UserResponse user,

        @Schema(description = "Random, 12 characters. The new administrator must change it at the first login")
        String temporaryPassword,

        @Schema(description = "The temporary password stops working after this moment if it was not changed")
        LocalDateTime passwordExpiresAt
) {
}
