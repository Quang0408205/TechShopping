package com.example.Tech.dto.request.payment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BankTransactionIgnoreRequest(

        @NotBlank(message = "Vui lòng nhập lý do bỏ qua")
        @Size(max = 500, message = "Ghi chú tối đa 500 ký tự")
        String note
) {
}
