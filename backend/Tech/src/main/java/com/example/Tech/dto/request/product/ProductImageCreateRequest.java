package com.example.Tech.dto.request.product;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record ProductImageCreateRequest(

        @NotNull(message = "Product is required")
        Long productId,

        @Schema(description = "Image URL or path",
                example = "https://cdn.tgdd.vn/Products/Images/42/370982/iphone-18-pro-max-do-thumb-600x600.jpg")
        @NotBlank(message = "Image URL is required")
        String imageUrl,

        @Size(max = 255, message = "Alt text must be at most 255 characters")
        String altText,

        @PositiveOrZero(message = "Display order must not be negative")
        Integer displayOrder,

        @Schema(description = "Defaults to false; true makes this the only primary image of the product")
        Boolean isPrimary
) {
}
