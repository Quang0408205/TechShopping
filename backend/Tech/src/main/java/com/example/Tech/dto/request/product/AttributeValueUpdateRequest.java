package com.example.Tech.dto.request.product;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Only the value text can change; the owning attribute is fixed.
 */
public record AttributeValueUpdateRequest(

        @Schema(example = "Đen nhám")
        @NotBlank(message = "Value is required")
        @Size(max = 255, message = "Value must be at most 255 characters")
        String value
) {
}
