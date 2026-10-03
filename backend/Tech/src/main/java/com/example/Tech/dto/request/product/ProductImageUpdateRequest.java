package com.example.Tech.dto.request.product;

import com.example.Tech.util.ImageUrlUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * The owning product cannot be changed.
 */
public record ProductImageUpdateRequest(

        @Schema(description = "http(s) URL")
        @NotBlank(message = "Image URL is required")
        @Size(max = ImageUrlUtil.MAX_IMAGE_URL_LENGTH, message = "Image URL must be at most 2048 characters")
        @Pattern(regexp = ImageUrlUtil.IMAGE_URL_REGEX, message = "Image URL must start with http:// or https://")
        String imageUrl,

        @Size(max = 255, message = "Alt text must be at most 255 characters")
        String altText,

        @PositiveOrZero(message = "Display order must not be negative")
        Integer displayOrder,

        @Schema(description = "Unchanged when omitted; true makes this the only primary image of the product. "
                + "false on the primary image is refused (PRIMARY_IMAGE_REQUIRED): set another image as primary instead")
        Boolean isPrimary
) {
}
