package com.example.Tech.dto.request.employee;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Replaces an employee profile. active = false (đã nghỉ) also ends the current store assignment.
 */
public record EmployeeUpdateRequest(

        @Size(max = 50, message = "Mã nhân viên tối đa 50 ký tự")
        String employeeCode,

        @Size(max = 100, message = "Bộ phận tối đa 100 ký tự")
        String department,

        @Size(max = 100, message = "Chức vụ tối đa 100 ký tự")
        String position,

        @PositiveOrZero(message = "Lương không được âm")
        BigDecimal salary,

        LocalDate hiringDate,

        @Schema(description = "false = đã nghỉ; null counts as true", example = "true")
        Boolean active
) {
}
