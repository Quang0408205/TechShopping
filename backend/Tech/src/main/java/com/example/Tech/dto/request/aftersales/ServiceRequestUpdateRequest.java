package com.example.Tech.dto.request.aftersales;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Staff action on a request: an optional status change plus the fields that go with it; null fields are kept. */
public record ServiceRequestUpdateRequest(
        @Schema(description = "Next status (empty = only update the other fields)", example = "RECEIVED")
        String status,

        @Size(max = 1000, message = "Lý do tối đa 1000 ký tự")
        @Schema(description = "Required when status = REJECTED; shown to the customer")
        String rejectionReason,

        @Size(max = 2000, message = "Ghi chú tối đa 2000 ký tự")
        @Schema(description = "Processing note shown to the customer (warranty / maintenance)")
        String notes,

        @Schema(description = "Warranty / maintenance", example = "2026-10-15")
        LocalDate estimatedCompletionDate,

        @PositiveOrZero(message = "Chi phí không được âm")
        @Schema(description = "Maintenance only", example = "300000")
        BigDecimal estimatedCost,

        @PositiveOrZero(message = "Chi phí không được âm")
        @Schema(description = "Maintenance only, usually with status COMPLETED", example = "250000")
        BigDecimal actualCost,

        @Schema(description = "Return with status RECEIVED: order lines put back into the store's stock")
        List<Long> restockOrderItemIds
) {
}
