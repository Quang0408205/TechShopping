package com.example.Tech.dto.response.payment;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** What the customer needs to make the transfer; only present while the transfer is awaited. */
public record BankTransferResponse(
        String bankName,
        String accountNumber,
        String accountName,
        BigDecimal amount,
        @Schema(description = "Text to put in the transfer: the order code", example = "DH00000042")
        String transferContent,
        @Schema(description = "VietQR image (img.vietqr.io) with the account, amount and content filled in")
        String qrImageUrl,
        @Schema(description = "Order date + the configured deadline (24 h); informative only")
        LocalDateTime payBefore
) {
}
