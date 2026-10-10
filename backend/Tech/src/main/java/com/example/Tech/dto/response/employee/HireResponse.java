package com.example.Tech.dto.response.employee;

import io.swagger.v3.oas.annotations.media.Schema;

/** The new employee plus the password to hand over; it is shown only in this response and never stored in clear. */
public record HireResponse(
        EmployeeResponse employee,

        @Schema(description = "STAFF or BRANCH_MANAGER", example = "STAFF")
        String role,

        @Schema(description = "Shown once; the employee should change it after the first login")
        String temporaryPassword
) {
}
