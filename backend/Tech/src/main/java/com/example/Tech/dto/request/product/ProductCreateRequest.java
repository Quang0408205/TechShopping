package com.example.Tech.dto.request.product;

import com.example.Tech.util.SlugUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ProductCreateRequest(

        @NotBlank(message = "Product name is required")
        @Size(max = 255, message = "Product name must be at most 255 characters")
        String name,

        @Schema(description = "Generated from name when omitted", example = "dien-thoai-iphone-18-pro-256gb")
        @Size(max = 255, message = "Slug must be at most 255 characters")
        @Pattern(regexp = SlugUtil.SLUG_REGEX, message = "Slug must contain only lowercase letters, digits and dashes")
        String slug,

        String description,

        @NotNull(message = "Category is required")
        Integer categoryId,

        @Schema(description = "Optional brand id")
        Integer brandId,

        @NotNull(message = "Base price is required")
        @DecimalMin(value = "0", message = "Base price must not be negative")
        @Digits(integer = 13, fraction = 2, message = "Base price must have at most 13 integer digits and 2 decimals")
        BigDecimal basePrice,

        @Schema(description = "Must not exceed basePrice")
        @DecimalMin(value = "0", message = "Discount price must not be negative")
        @Digits(integer = 13, fraction = 2, message = "Discount price must have at most 13 integer digits and 2 decimals")
        BigDecimal discountPrice,

        @Schema(description = "Defaults to 0 when omitted")
        @PositiveOrZero(message = "Stock quantity must not be negative")
        Integer stockQuantity,

        @Size(max = 50, message = "SKU must be at most 50 characters")
        String sku,

        @DecimalMin(value = "0", message = "Weight must not be negative")
        @Digits(integer = 8, fraction = 2, message = "Weight must have at most 8 integer digits and 2 decimals")
        BigDecimal weight,

        @Schema(description = "Defaults to 12 when omitted")
        @PositiveOrZero(message = "Warranty months must not be negative")
        Integer warrantyMonths,

        @Schema(description = "Defaults to true when omitted")
        Boolean isActive
) {
}
