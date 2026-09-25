package com.example.Tech.dto.request.product;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * The owning product cannot be changed.
 */
public record ProductSpecificationUpdateRequest(

        @Schema(example = "Màn hình")
        @NotBlank(message = "Specification name is required")
        @Size(max = 100, message = "Specification name must be at most 100 characters")
        String specName,

        @Schema(example = "6.2 inch, Dynamic AMOLED 2X, 120Hz")
        @NotBlank(message = "Specification value is required")
        String specValue,

        @PositiveOrZero(message = "Specification order must not be negative")
        Integer specOrder
) {
}
