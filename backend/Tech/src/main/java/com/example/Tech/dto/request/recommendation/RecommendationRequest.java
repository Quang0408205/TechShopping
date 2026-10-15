package com.example.Tech.dto.request.recommendation;

import com.example.Tech.entity.recommendation.RecommendationType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record RecommendationRequest(

        @NotNull(message = "Recommendation type is required")
        @Schema(description = "PERSONAL (for the current user, popular for guests), SIMILAR, UPSELL, CROSS_SELL",
                example = "PERSONAL")
        RecommendationType type,

        @Schema(description = "Product being viewed; required for SIMILAR, UPSELL and CROSS_SELL", example = "11")
        Long productId,

        @Min(value = 1, message = "Size must be at least 1")
        @Max(value = 20, message = "Size must be at most 20")
        @Schema(description = "Number of products (default 8)", example = "8")
        Integer size
) {

    public int sizeOrDefault() {
        return size != null ? size : 8;
    }
}
