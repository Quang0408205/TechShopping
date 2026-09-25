package com.example.Tech.dto.request.product;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ProductVariantCreateRequest(

        @NotNull(message = "Product is required")
        Long productId,

        @NotBlank(message = "Variant name is required")
        @Size(max = 255, message = "Variant name must be at most 255 characters")
        String variantName,

        @Size(max = 50, message = "Variant SKU must be at most 50 characters")
        String skuVariant,

        @NotNull(message = "Price is required")
        @DecimalMin(value = "0", message = "Price must not be negative")
        @Digits(integer = 13, fraction = 2, message = "Price must have at most 13 integer digits and 2 decimals")
        BigDecimal price,

        @Schema(description = "Must not exceed price")
        @DecimalMin(value = "0", message = "Discount price must not be negative")
        @Digits(integer = 13, fraction = 2, message = "Discount price must have at most 13 integer digits and 2 decimals")
        BigDecimal discountPrice,

        @Schema(description = "Defaults to 0 when omitted")
        @PositiveOrZero(message = "Stock quantity must not be negative")
        Integer stockQuantity,

        @Size(max = 50, message = "Color must be at most 50 characters")
        String color,

        @Schema(example = "256 GB")
        @Size(max = 50, message = "Storage must be at most 50 characters")
        String storage,

        @Schema(example = "8 GB")
        @Size(max = 50, message = "RAM must be at most 50 characters")
        String ram
) {
}
