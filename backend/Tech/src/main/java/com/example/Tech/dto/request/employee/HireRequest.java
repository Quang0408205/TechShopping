package com.example.Tech.dto.request.employee;

import com.example.Tech.util.AccountUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Hires a new internal account: the user, the employee profile and the store assignment are created together.
 * ADMIN must send role and storeId; a branch manager's role must be STAFF or empty and storeId is ignored (the
 * new employee always joins the manager's own store).
 */
public record HireRequest(

        @NotBlank(message = "Vui lòng nhập email")
        @Email(message = "Email không hợp lệ")
        @Size(max = 120, message = "Email tối đa 120 ký tự")
        @Schema(example = "an.nguyen@poy.vn")
        String email,

        @NotBlank(message = "Vui lòng nhập tên đăng nhập")
        @Size(min = 3, max = 50, message = "Tên đăng nhập từ 3 đến 50 ký tự")
        @Pattern(regexp = AccountUtil.USERNAME_REGEX, message = "Tên đăng nhập chỉ gồm chữ, số, dấu chấm, gạch dưới và gạch ngang")
        @Schema(example = "an.nguyen")
        String username,

        @NotBlank(message = "Vui lòng nhập họ tên")
        @Size(max = 120, message = "Họ tên tối đa 120 ký tự")
        @Schema(example = "Nguyễn Văn An")
        String fullname,

        @Size(max = 20, message = "Số điện thoại tối đa 20 ký tự")
        @Schema(example = "0901234567")
        String phone,

        @Size(min = AccountUtil.MIN_PASSWORD_LENGTH, max = AccountUtil.MAX_PASSWORD_BYTES,
                message = "Mật khẩu tạm từ 8 đến 72 ký tự")
        @Schema(description = "Optional; empty = a random 12-character password. Returned once in the response")
        String temporaryPassword,

        @Schema(description = "STAFF or BRANCH_MANAGER (ADMIN only; a manager can only hire STAFF)", example = "STAFF")
        String role,

        @Schema(description = "ADMIN only; ignored for a branch manager", example = "1")
        Integer storeId,

        @Size(max = 100, message = "Vị trí tại chi nhánh tối đa 100 ký tự")
        @Schema(description = "Display label; \"Quản lý chi nhánh\" is reserved for the BRANCH_MANAGER role",
                example = "Nhân viên bán hàng")
        String positionAtStore,

        @Size(max = 50, message = "Mã nhân viên tối đa 50 ký tự")
        @Schema(example = "NV001")
        String employeeCode,

        @Size(max = 100, message = "Bộ phận tối đa 100 ký tự")
        String department,

        @Size(max = 100, message = "Chức vụ tối đa 100 ký tự")
        String position,

        @PositiveOrZero(message = "Lương không được âm")
        BigDecimal salary,

        @Schema(example = "2026-10-01")
        LocalDate hiringDate
) {
}
