package com.example.Tech.dto.request.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

/**
 * Optional filters for the admin user list; all conditions are combined with AND.
 */
public record UserSearchRequest(

        @Schema(description = "Matches email, username or full name (case-insensitive)", example = "nguyen")
        @Size(max = 255, message = "Keyword must be at most 255 characters")
        String keyword,

        @Schema(description = "Role name", example = "CUSTOMER")
        @Size(max = 50, message = "Role must be at most 50 characters")
        String role,

        @Schema(description = "Active flag")
        Boolean isActive,

        @Schema(description = "Also list soft-deleted users (default false)")
        Boolean includeDeleted
) {
}
