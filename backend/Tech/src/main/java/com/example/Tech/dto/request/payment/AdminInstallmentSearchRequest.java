package com.example.Tech.dto.request.payment;

import com.example.Tech.entity.payment.InstallmentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

/** Filters of the staff installment list; all optional, combined with AND. */
public record AdminInstallmentSearchRequest(

        @Schema(description = "Order code / id, recipient name or phone, customer email / username / name, citizen id")
        @Size(max = 255, message = "Từ khoá tối đa 255 ký tự")
        String keyword,

        InstallmentStatus status,

        @Schema(description = "true: only plans with an unpaid period past its due date")
        Boolean overdue
) {
}
