package com.example.Tech.dto.response.combo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "A combo running now for the main product, as the product page shows it (public)")
public record ComboOptionResponse(
        Long id,
        String name,
        String description,
        LocalDateTime endsAt,
        List<Item> items
) {

    public record Item(
            Long productId,
            String name,
            String slug,
            String imageUrl,
            String discountType,
            BigDecimal discountValue,
            List<Variant> variants
    ) {
    }

    @Schema(description = "A variant of the add-on: price now and the price with the combo discount; cheapest first")
    public record Variant(
            Long id,
            String label,
            String color,
            BigDecimal price,
            BigDecimal comboPrice,
            Integer availableQuantity
    ) {
    }
}
