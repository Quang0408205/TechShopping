package com.example.Tech.dto.response.employee;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record EmployeeResponse(
        Long id,
        Long userId,
        String username,
        String email,
        String fullname,
        String phone,
        String employeeCode,
        String department,
        String position,
        BigDecimal salary,
        LocalDate hiringDate,
        boolean active,

        @Schema(description = "Current store assignment; null when the employee is not assigned to any store")
        CurrentAssignment assignment,

        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public record CurrentAssignment(
            Long id,
            Integer storeId,
            String storeName,
            String positionAtStore,
            LocalDate startDate
    ) {
    }
}
