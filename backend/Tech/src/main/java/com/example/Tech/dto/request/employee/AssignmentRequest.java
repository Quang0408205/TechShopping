package com.example.Tech.dto.request.employee;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Moves an employee to a store: the current assignment (if any) ends and a new one starts.
 */
public record AssignmentRequest(

        @NotNull(message = "Vui lòng chọn chi nhánh")
        @Schema(example = "1")
        Integer storeId,

        @Size(max = 100, message = "Vị trí tại chi nhánh tối đa 100 ký tự")
        @Schema(example = "Quản lý chi nhánh")
        String positionAtStore,

        @Schema(description = "Default today", example = "2026-10-03")
        LocalDate startDate
) {
}
