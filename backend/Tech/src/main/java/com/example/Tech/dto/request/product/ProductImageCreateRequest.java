package com.example.Tech.dto.request.product;

import com.example.Tech.util.ImageUrlUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record ProductImageCreateRequest(

        @NotNull(message = "Product is required")
        Long productId,

        @Schema(description = "http(s) URL, e.g. one returned by POST /api/v1/admin/uploads/product-images",
                example = "https://cdn.tgdd.vn/Products/Images/42/370982/iphone-18-pro-max-do-thumb-600x600.jpg")
        @NotBlank(message = "Image URL is required")
        @Size(max = ImageUrlUtil.MAX_IMAGE_URL_LENGTH, message = "Image URL must be at most 2048 characters")
        @Pattern(regexp = ImageUrlUtil.IMAGE_URL_REGEX, message = "Image URL must start with http:// or https://")
        String imageUrl,

        @Size(max = 255, message = "Alt text must be at most 255 characters")
        String altText,

        @PositiveOrZero(message = "Display order must not be negative")
        Integer displayOrder,

        @Schema(description = "Defaults to false; true makes this the only primary image of the product. "
                + "The first image of a product without a primary image always becomes primary")
        Boolean isPrimary
) {
}
