package com.example.Tech.dto.request.employee;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Creates the employee profile of an existing STAFF account, optionally assigning it to a store at once.
 */
public record EmployeeCreateRequest(

        @NotNull(message = "Vui lòng chọn tài khoản")
        @Schema(description = "An existing account with the STAFF role", example = "12")
        Long userId,

        @Size(max = 50, message = "Mã nhân viên tối đa 50 ký tự")
        @Schema(example = "NV001")
        String employeeCode,

        @Size(max = 100, message = "Bộ phận tối đa 100 ký tự")
        @Schema(example = "Bán hàng")
        String department,

        @Size(max = 100, message = "Chức vụ tối đa 100 ký tự")
        @Schema(example = "Nhân viên bán hàng")
        String position,

        @PositiveOrZero(message = "Lương không được âm")
        @Schema(example = "8000000")
        BigDecimal salary,

        @Schema(example = "2026-10-01")
        LocalDate hiringDate,

        @Schema(description = "Assign to this store right away (optional)", example = "1")
        Integer storeId,

        @Size(max = 100, message = "Vị trí tại chi nhánh tối đa 100 ký tự")
        @Schema(description = "\"Quản lý chi nhánh\" / \"Trưởng ...\" is shown as branch manager in the admin area",
                example = "Nhân viên")
        String positionAtStore
) {
}
