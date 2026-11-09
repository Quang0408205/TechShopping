package com.example.Tech.dto.request.recommendation;

import com.example.Tech.entity.recommendation.RecommendationType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record RecommendationRequest(

        @NotNull(message = "Recommendation type is required")
        @Schema(description = "PERSONAL (for the current user; a customer without activity gets products matching "
                + "the questionnaire answers), SIMILAR, UPSELL, CROSS_SELL",
                example = "PERSONAL")
        RecommendationType type,

        @Schema(description = "Product being viewed; required for SIMILAR, UPSELL and CROSS_SELL", example = "11")
        Long productId,

        @Min(value = 1, message = "Size must be at least 1")
        @Max(value = 20, message = "Size must be at most 20")
        @Schema(description = "Number of products (default 8)", example = "8")
        Integer size,

        @Valid
        @Schema(description = "PERSONAL for a guest: questionnaire answers kept in the browser (ignored with a token: "
                + "the answers saved in the account are used)")
        PreferenceRequest preferences
) {

    public RecommendationRequest(RecommendationType type, Long productId, Integer size) {
        this(type, productId, size, null);
    }

    public int sizeOrDefault() {
        return size != null ? size : 8;
    }
}
