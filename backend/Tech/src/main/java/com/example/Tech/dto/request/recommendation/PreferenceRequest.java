package com.example.Tech.dto.request.recommendation;

import com.example.Tech.entity.recommendation.PriceLevel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Answers to the shopping-preference questionnaire. Saved for a logged-in customer
 * (PUT /api/v1/users/me/preferences); a guest's browser sends them with POST /api/v1/recommendations.
 */
public record PreferenceRequest(

        @NotEmpty(message = "Choose at least one category")
        @Size(max = 20, message = "At most 20 categories")
        @Schema(description = "Categories the customer is interested in (required)", example = "[4, 5]")
        List<@NotNull Integer> categoryIds,

        @Size(max = 60, message = "At most 60 brands")
        @Schema(description = "Preferred brands; empty = any brand", example = "[1, 3]")
        List<@NotNull Integer> brandIds,

        @Size(max = 3, message = "At most 3 price levels")
        @Schema(description = "Price levels inside each category: LOW, MID, HIGH; empty = any price",
                example = "[\"MID\"]")
        List<@NotNull PriceLevel> priceLevels,

        @Size(max = 40, message = "At most 40 needs")
        @Schema(description = "Needs (gaming, photography, office work...) of the chosen categories; empty = any",
                example = "[2]")
        List<@NotNull Integer> needIds
) {

    public PreferenceRequest {
        categoryIds = distinct(categoryIds);
        brandIds = distinct(brandIds);
        priceLevels = distinct(priceLevels);
        needIds = distinct(needIds);
    }

    private static <T> List<T> distinct(List<T> values) {
        return values == null ? List.of() : values.stream().distinct().toList();
    }
}
