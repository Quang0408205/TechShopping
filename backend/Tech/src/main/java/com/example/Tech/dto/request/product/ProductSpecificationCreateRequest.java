package com.example.Tech.dto.request.product;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record ProductSpecificationCreateRequest(

        @NotNull(message = "Product is required")
        Long productId,

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
