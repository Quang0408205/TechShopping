package com.example.Tech.dto.request.review;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * Optional filters of a product's review list.
 */
public record ReviewSearchRequest(

        @Min(value = 1, message = "Số sao từ 1 đến 5")
        @Max(value = 5, message = "Số sao từ 1 đến 5")
        @Schema(description = "Only reviews with this many stars", example = "5")
        Integer rating,

        @Schema(description = "true = only reviews with photos", example = "false")
        Boolean withImages
) {
}
