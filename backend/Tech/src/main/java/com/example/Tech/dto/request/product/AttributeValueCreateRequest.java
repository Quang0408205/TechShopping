package com.example.Tech.dto.request.product;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AttributeValueCreateRequest(

        @NotNull(message = "Attribute is required")
        Integer attributeId,

        @Schema(example = "Đen")
        @NotBlank(message = "Value is required")
        @Size(max = 255, message = "Value must be at most 255 characters")
        String value
) {
}
