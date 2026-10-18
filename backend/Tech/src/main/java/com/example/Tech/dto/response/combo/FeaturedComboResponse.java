package com.example.Tech.dto.response.combo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "A running combo as the home page shows it: main product + add-ons at their cheapest variant (public)")
public record FeaturedComboResponse(
        Long id,
        String name,
        LocalDateTime endsAt,
        Product main,
        List<AddOn> items,
        @Schema(description = "Main product + every add-on at the normal price") BigDecimal originalTotal,
        @Schema(description = "The same basket with the combo discount") BigDecimal comboTotal,
        BigDecimal saved
) {

    public record Product(Long productId, String name, String imageUrl, BigDecimal price) {
    }

    public record AddOn(Long productId, String name, String imageUrl, BigDecimal price, BigDecimal comboPrice) {
    }
}
