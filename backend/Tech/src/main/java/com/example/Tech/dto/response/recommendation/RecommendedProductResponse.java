package com.example.Tech.dto.response.recommendation;

import com.example.Tech.dto.response.product.ProductResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

public record RecommendedProductResponse(

        ProductResponse product,

        @Schema(description = "Hybrid / relevance score in [0, 1]; null for fallback lists", example = "0.873")
        BigDecimal score,

        @Schema(description = "Why the product is suggested, shown under the card", example = "Cùng hãng Apple, cùng tầm giá")
        String reason
) {
}
