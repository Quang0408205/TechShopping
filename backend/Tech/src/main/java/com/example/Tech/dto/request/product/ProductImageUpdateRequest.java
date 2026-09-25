package com.example.Tech.dto.request.product;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * The owning product cannot be changed.
 */
public record ProductImageUpdateRequest(

        @Schema(description = "Image URL or path")
        @NotBlank(message = "Image URL is required")
        String imageUrl,

        @Size(max = 255, message = "Alt text must be at most 255 characters")
        String altText,

        @PositiveOrZero(message = "Display order must not be negative")
        Integer displayOrder,

        @Schema(description = "Unchanged when omitted; true makes this the only primary image of the product")
        Boolean isPrimary
) {
}
