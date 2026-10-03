package com.example.Tech.dto.request.promotion;

import com.example.Tech.entity.promotion.PromotionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * Optional filters for the promotion list; all conditions are combined with AND.
 */
public record PromotionSearchRequest(

        @Schema(description = "Part of the promotion name", example = "flash sale")
        @Size(max = 255, message = "Từ khóa tối đa 255 ký tự")
        String keyword,

        @Schema(description = "Status at the time of the request", example = "RUNNING")
        PromotionStatus status,

        @Schema(description = "Promotions still running on or after this day (yyyy-MM-dd)", example = "2026-10-01")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate fromDate,

        @Schema(description = "Promotions starting on or before this day (yyyy-MM-dd)", example = "2026-10-31")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate toDate
) {
}
