package com.example.Tech.dto.request.voucher;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record VoucherSearchRequest(

        @Schema(description = "Part of the code or the name")
        @Size(max = 100, message = "Từ khoá tối đa 100 ký tự")
        String keyword,

        @Schema(description = "RUNNING, UPCOMING, ENDED or PAUSED")
        @Pattern(regexp = "RUNNING|UPCOMING|ENDED|PAUSED", message = "Trạng thái không hợp lệ")
        String status
) {
}
