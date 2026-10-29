package com.example.Tech.dto.request.payment;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * One bank balance change, in the field names SePay uses so a real provider can be plugged in later:
 * {@code id} (bank / provider transaction id), {@code transferType} ("in" = money received),
 * {@code transferAmount}, {@code content}, {@code accountNumber}, {@code transactionDate}.
 */
public record BankWebhookRequest(

        @NotBlank(message = "Mã giao dịch là bắt buộc")
        @Size(max = 100, message = "Mã giao dịch tối đa 100 ký tự")
        @Schema(description = "Unique transaction id; the same id sent twice is stored once", example = "FT26289123456")
        String id,

        @Size(max = 30, message = "Tên nhà cung cấp tối đa 30 ký tự")
        @Schema(example = "Vietcombank")
        String gateway,

        @Schema(description = "Time of the transfer, yyyy-MM-dd HH:mm:ss", example = "2026-10-16 21:30:00")
        String transactionDate,

        @Size(max = 30, message = "Số tài khoản tối đa 30 ký tự")
        @Schema(description = "Account that received the money", example = "0123456789")
        String accountNumber,

        @Size(max = 500, message = "Nội dung tối đa 500 ký tự")
        @Schema(description = "Transfer content; the order code (DH00000042) is read from it", example = "DH00000042 chuyen tien")
        String content,

        @NotBlank(message = "Loại giao dịch là bắt buộc")
        @Schema(description = "in = money received, out = money sent (ignored)", example = "in")
        String transferType,

        @NotNull(message = "Số tiền là bắt buộc")
        @DecimalMin(value = "0.01", message = "Số tiền phải lớn hơn 0")
        @Schema(example = "29990000")
        BigDecimal transferAmount
) {
}
