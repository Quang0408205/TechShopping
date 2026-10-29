package com.example.Tech.dto.request.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * Optional filters for the admin user list; all conditions are combined with AND.
 */
public record UserSearchRequest(

        @Schema(description = "Matches email, username or full name (case-insensitive)", example = "nguyen")
        @Size(max = 255, message = "Keyword must be at most 255 characters")
        String keyword,

        @Schema(description = "Role name, or INTERNAL for any internal account (STAFF, BRANCH_MANAGER, ADMIN)", example = "CUSTOMER")
        @Size(max = 50, message = "Role must be at most 50 characters")
        String role,

        @Schema(description = "Active flag")
        Boolean isActive,

        @Schema(description = "Also list soft-deleted users (default false)")
        Boolean includeDeleted,
        @Schema(description = "Accounts created on or after this day (yyyy-MM-dd)", example = "2026-10-01")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate createdFrom,
        @Schema(description = "Accounts created on or before this day (yyyy-MM-dd, the whole day counts)", example = "2026-10-31")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate createdTo
) {
}
