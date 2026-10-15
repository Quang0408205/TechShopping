package com.example.Tech.dto.request.payment;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

/** Optional body when staff records money received (bank transfer or an installment period). */
public record PaymentConfirmRequest(

        @Schema(description = "Bank reference of the transfer (optional)", example = "FT26276000123")
        @Size(max = 100, message = "Mã giao dịch tối đa 100 ký tự")
        String transactionId
) {
}
