package com.example.Tech.dto.request.product;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Optional filters for the product list; all conditions are combined with AND.
 */
public record ProductSearchRequest(

        @Schema(description = "Matches the product name (case-insensitive) or its slug, so accents are optional",
                example = "dien thoai iphone")
        @Size(max = 255, message = "Keyword must be at most 255 characters")
        String keyword,

        @Schema(description = "Exact category id")
        Integer categoryId,

        @Schema(description = "Exact brand id")
        Integer brandId,

        @Schema(description = "Active flag")
        Boolean isActive,

        @Schema(description = "Minimum base price (inclusive)", example = "5000000")
        @DecimalMin(value = "0", message = "Minimum price must not be negative")
        BigDecimal minPrice,

        @Schema(description = "Maximum base price (inclusive)", example = "20000000")
        @DecimalMin(value = "0", message = "Maximum price must not be negative")
        BigDecimal maxPrice
) {
}
