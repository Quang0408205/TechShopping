package com.example.Tech.dto.response.promotion;

import com.example.Tech.dto.response.product.ProductResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "Home page flash sale: products under a running promotion, deepest discount first (public)")
public record FlashSaleResponse(
        @Schema(description = "When the soonest-ending promotion among the items ends (countdown); null when items is empty")
        LocalDateTime endsAt,
        List<ProductResponse> items
) {

    public static FlashSaleResponse empty() {
        return new FlashSaleResponse(null, List.of());
    }
}
