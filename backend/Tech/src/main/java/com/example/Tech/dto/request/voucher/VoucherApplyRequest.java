package com.example.Tech.dto.request.voucher;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VoucherApplyRequest(

        @Schema(description = "Voucher code, any case", example = "POY10")
        @NotBlank(message = "Vui lòng nhập mã voucher")
        @Size(max = 30, message = "Mã voucher tối đa 30 ký tự")
        String code
) {
}
