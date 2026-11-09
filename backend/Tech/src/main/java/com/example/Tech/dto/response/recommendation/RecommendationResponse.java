package com.example.Tech.dto.response.recommendation;

import com.example.Tech.entity.recommendation.RecommendationType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record RecommendationResponse(

        RecommendationType type,

        @Schema(description = "AI: computed by the recommendation models; FALLBACK: the AI service did not answer, "
                + "popular / same-category products are returned instead")
        String source,

        List<RecommendedProductResponse> items,

        @Schema(description = "PERSONAL from the AI service: BEHAVIOR (the customer's own activity), PREFERENCES "
                + "(no activity yet: questionnaire answers), POPULAR (neither: the website asks the questionnaire "
                + "instead of showing this list); null for other types and FALLBACK")
        String basis
) {

    public RecommendationResponse(RecommendationType type, String source, List<RecommendedProductResponse> items) {
        this(type, source, items, null);
    }
}
