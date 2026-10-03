package com.example.Tech.dto.request.order;

import com.example.Tech.entity.order.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * Optional filters for the staff order list; all conditions are combined with AND.
 */
public record AdminOrderSearchRequest(

        @Schema(description = "Order code (DH00000042) or id, or part of the recipient name / phone, "
                + "customer email / username / name", example = "DH00000042")
        @Size(max = 255, message = "Keyword must be at most 255 characters")
        String keyword,

        @Schema(description = "Order status", example = "PENDING")
        OrderStatus status,

        @Schema(description = "Orders placed on or after this day (yyyy-MM-dd)", example = "2026-10-01")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate fromDate,

        @Schema(description = "Orders placed on or before this day (yyyy-MM-dd)", example = "2026-10-31")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate toDate
) {
}
