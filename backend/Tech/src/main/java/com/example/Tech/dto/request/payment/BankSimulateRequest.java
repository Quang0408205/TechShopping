package com.example.Tech.dto.request.payment;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** A fake "money arrived" notice for demos; goes through the same code as the real webhook. */
public record BankSimulateRequest(

        @NotBlank(message = "Vui lòng nhập nội dung chuyển khoản")
        @Size(max = 500, message = "Nội dung tối đa 500 ký tự")
        String content,

        @NotNull(message = "Số tiền là bắt buộc")
        @DecimalMin(value = "0.01", message = "Số tiền phải lớn hơn 0")
        BigDecimal amount
) {
}
