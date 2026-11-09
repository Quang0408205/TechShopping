package com.example.Tech.dto.response.recommendation;

import com.example.Tech.entity.recommendation.PriceLevel;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record PreferenceResponse(

        @Schema(description = "The customer has answered the questionnaire (at least one category)")
        boolean answered,

        @Schema(description = "The customer already viewed, liked, carted or bought something: recommendations then "
                + "follow that activity and the questionnaire is not required")
        boolean hasActivity,

        List<Integer> categoryIds,

        List<Integer> brandIds,

        List<PriceLevel> priceLevels,

        List<Integer> needIds
) {
}
