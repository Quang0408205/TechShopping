package com.example.Tech.dto.request.inventory;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * Filters of the goods-received statistics (ADMIN). Every field is optional.
 */
public record StockInStatsRequest(

        @Schema(description = "Only this store (null = every store)", example = "1")
        Integer storeId,

        @Schema(description = "Stock-ins on or after this day (yyyy-MM-dd)", example = "2026-10-01")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate fromDate,

        @Schema(description = "Stock-ins on or before this day (yyyy-MM-dd)", example = "2026-10-31")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate toDate
) {
}
