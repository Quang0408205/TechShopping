package com.example.Tech.dto.request.report;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/** Filters of the sales report. Every field is optional; a branch manager is always limited to their store. */
public record SalesReportRequest(

        @Schema(description = "Only this store (ADMIN; null = every store)", example = "1")
        Integer storeId,

        @Schema(description = "Delivered / refunded on or after this day (default: 29 days before toDate)",
                example = "2026-10-01")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate fromDate,

        @Schema(description = "Delivered / refunded on or before this day (default: today)", example = "2026-10-31")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate toDate,

        @Schema(description = "DAY or MONTH (default: DAY up to 62 days, MONTH beyond)")
        ReportGroupBy groupBy
) {
}
