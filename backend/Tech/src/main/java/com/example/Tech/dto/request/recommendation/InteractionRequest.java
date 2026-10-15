package com.example.Tech.dto.request.recommendation;

import com.example.Tech.entity.recommendation.InteractionType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record InteractionRequest(

        @NotNull(message = "Product id is required")
        @Schema(example = "12")
        Long productId,

        @NotNull(message = "Interaction type is required")
        @Schema(description = "VIEW (opened the detail page) or CLICK (picked the product in a list)",
                allowableValues = {"VIEW", "CLICK"}, example = "VIEW")
        InteractionType type
) {
}
