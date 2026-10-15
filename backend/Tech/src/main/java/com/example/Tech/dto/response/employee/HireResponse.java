package com.example.Tech.dto.response.employee;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/** The new employee plus the password to hand over; it is shown only in this response and never stored in clear. */
public record HireResponse(
        EmployeeResponse employee,

        @Schema(description = "STAFF or BRANCH_MANAGER", example = "STAFF")
        String role,

        @Schema(description = "Always the default password 1 (EmployeeHiringServiceImpl). The employee must change it at the first login")
        String temporaryPassword,

        @Schema(description = "The temporary password stops working after this moment if it was not changed")
        LocalDateTime passwordExpiresAt
) {
}
