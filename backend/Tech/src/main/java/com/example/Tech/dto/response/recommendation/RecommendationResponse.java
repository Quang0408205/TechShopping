package com.example.Tech.dto.response.recommendation;

import com.example.Tech.entity.recommendation.RecommendationType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record RecommendationResponse(

        RecommendationType type,

        @Schema(description = "AI: computed by the recommendation models; FALLBACK: the AI service did not answer, "
                + "popular / same-category products are returned instead")
        String source,

        List<RecommendedProductResponse> items
) {
}
