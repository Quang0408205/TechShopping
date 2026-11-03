package com.example.Tech.dto.response.recommendation;

import com.example.Tech.entity.recommendation.RecommendationType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** Admin page "Gợi ý sản phẩm": the model in use, the offline evaluation and how the lists do on the site. */
public record RecommendationOverviewResponse(

        @Schema(description = "False when the AI service did not answer (model and evaluation are then null)")
        boolean aiAvailable,

        @Schema(description = "AI GET /health: trainedAt, products, users, interactions, features, co_purchase_items...")
        Map<String, Object> model,

        @Schema(description = "AI GET /evaluation: running, startedAt, error, result (rows by model / α, bestAlpha...)")
        Map<String, Object> evaluation,

        @Schema(description = "Online figures cover the recommendations shown in this many last days")
        int days,

        List<OnlineStat> online
) {

    public record OnlineStat(

            RecommendationType type,

            @Schema(description = "Products shown to logged-in customers in this kind of list")
            long impressions,

            @Schema(description = "Of those, picked by the customer")
            long clicks,

            @Schema(description = "Different customers who were shown such a list")
            long users,

            @Schema(description = "Clicks followed by adding that product to the cart or buying it within 7 days")
            long converted,

            @Schema(description = "clicks / impressions (0..1), null without impressions")
            BigDecimal clickRate,

            @Schema(description = "converted / clicks (0..1), null without clicks")
            BigDecimal conversionRate
    ) {
    }
}
