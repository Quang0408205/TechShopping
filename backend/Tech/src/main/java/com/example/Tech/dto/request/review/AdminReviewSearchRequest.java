package com.example.Tech.dto.request.review;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * Optional filters of the admin review list.
 */
public record AdminReviewSearchRequest(

        @Size(max = 255, message = "Từ khóa tối đa 255 ký tự")
        @Schema(description = "Part of the product name, the author's name / username / email, or the comment",
                example = "pin")
        String keyword,

        @Min(value = 1, message = "Số sao từ 1 đến 5")
        @Max(value = 5, message = "Số sao từ 1 đến 5")
        @Schema(example = "1")
        Integer rating,

        @Schema(description = "true = only hidden reviews, false = only visible ones, empty = all")
        Boolean hidden,

        @Schema(description = "Only reviews of this product", example = "1")
        Long productId
) {
}
