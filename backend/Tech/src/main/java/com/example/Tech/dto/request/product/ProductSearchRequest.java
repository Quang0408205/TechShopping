package com.example.Tech.dto.request.product;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

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
        BigDecimal maxPrice,

        @Schema(description = "Spec filters group:value, repeatable (same group = OR, different groups = AND); "
                + "groups and values come from GET /categories/{id}/filters", example = "ram:8GB")
        List<String> filter,

        @Schema(description = "Need slugs of the category, repeatable (any of them)", example = "gaming")
        List<String> need,

        @Schema(description = "true = only products on sale now: a manual discount price (product or variant) or a running promotion")
        Boolean onSale
) {

    public ProductSearchRequest(String keyword, Integer categoryId, Integer brandId, Boolean isActive,
                                BigDecimal minPrice, BigDecimal maxPrice, List<String> filter, List<String> need) {
        this(keyword, categoryId, brandId, isActive, minPrice, maxPrice, filter, need, null);
    }

    public ProductSearchRequest(String keyword, Integer categoryId, Integer brandId, Boolean isActive,
                                BigDecimal minPrice, BigDecimal maxPrice) {
        this(keyword, categoryId, brandId, isActive, minPrice, maxPrice, null, null);
    }
}
