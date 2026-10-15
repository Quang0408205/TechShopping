package com.example.Tech.dto.request.product;

import com.example.Tech.util.ImageUrlUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * One image sent together with a new product. The list order becomes the display order.
 */
public record ProductImageInput(

        @Schema(description = "http(s) URL, e.g. one returned by POST /api/v1/admin/uploads/product-images",
                example = "https://cdn.tgdd.vn/Products/Images/42/370982/iphone-18-pro-max-do-thumb-600x600.jpg")
        @NotBlank(message = "Image URL is required")
        @Size(max = ImageUrlUtil.MAX_IMAGE_URL_LENGTH, message = "Image URL must be at most 2048 characters")
        @Pattern(regexp = ImageUrlUtil.IMAGE_URL_REGEX, message = "Image URL must start with http:// or https://")
        String imageUrl,

        @Size(max = 255, message = "Alt text must be at most 255 characters")
        String altText,

        @Schema(description = "Exactly one image of the list must be primary")
        Boolean isPrimary
) {
}
