package com.example.Tech.dto.request.order;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RefundAccountRequest(

        @NotBlank(message = "Vui lòng chọn ngân hàng nhận hoàn tiền")
        @Size(max = 100, message = "Tên ngân hàng tối đa 100 ký tự")
        String bankName,

        @NotBlank(message = "Vui lòng nhập số tài khoản")
        @Pattern(regexp = "^[0-9]{6,20}$", message = "Số tài khoản gồm 6 đến 20 chữ số")
        String accountNumber,

        @NotBlank(message = "Vui lòng nhập tên chủ tài khoản")
        @Size(max = 100, message = "Tên chủ tài khoản tối đa 100 ký tự")
        String accountHolder
) {
}
