package com.example.Tech.dto.request.employee;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * What a branch manager may change on a STAFF member of their own store. Leaving a store, the role and the
 * account status are not part of it (leaving = the deactivate endpoint).
 */
public record BranchEmployeeUpdateRequest(

        @Size(max = 50, message = "Mã nhân viên tối đa 50 ký tự")
        String employeeCode,

        @Size(max = 100, message = "Bộ phận tối đa 100 ký tự")
        String department,

        @Size(max = 100, message = "Chức vụ tối đa 100 ký tự")
        String position,

        @PositiveOrZero(message = "Lương không được âm")
        BigDecimal salary,

        LocalDate hiringDate,

        @Size(max = 100, message = "Vị trí tại chi nhánh tối đa 100 ký tự")
        @Schema(description = "Empty keeps the current label; \"Quản lý chi nhánh\" is refused", example = "Thu ngân")
        String positionAtStore
) {
}
