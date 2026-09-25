package com.example.Tech.dto.request.product;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AttributeUpdateRequest(

        @Schema(example = "Màu sắc")
        @NotBlank(message = "Attribute name is required")
        @Size(max = 100, message = "Attribute name must be at most 100 characters")
        String name,

        String description,

        @Schema(description = "Free-text type as stored in the schema", example = "color")
        @Size(max = 50, message = "Attribute type must be at most 50 characters")
        String attributeType
) {
}
