package com.example.Tech.dto.request.payment;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record InstallmentRejectRequest(

        @Schema(description = "Shown to the customer", example = "Không xác minh được thông tin CCCD")
        @NotBlank(message = "Vui lòng nhập lý do từ chối")
        @Size(max = 1000, message = "Lý do tối đa 1000 ký tự")
        String reason
) {
}
