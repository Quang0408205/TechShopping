package com.example.Tech.dto.response.excel;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;

/**
 * Result of an Excel import. Every row is checked first: with any error nothing is written (applied = false).
 * dryRun = true runs the whole import and rolls it back, so the summary is exactly what a real import would do.
 */
public record ExcelImportResponse(
        boolean dryRun,
        @Schema(description = "True only when the data was written (not a dry run and no error)")
        boolean applied,
        @Schema(description = "Data rows read (blank rows skipped)")
        int rowCount,
        @Schema(description = "What the import does / did, e.g. productsCreated, variantsUpdated", example = "{\"productsCreated\": 2}")
        Map<String, Integer> summary,
        List<Error> errors
) {

    public record Error(String sheet, int row, String column, String message) {
    }
}
