package com.example.Tech.dto.response.combo;

import java.math.BigDecimal;

/** An add-on idea for a main product from the AI "bought together" relations (product_relations). */
public record ComboSuggestionResponse(
        Long productId,
        String name,
        String slug,
        String imageUrl,
        BigDecimal price,
        String relationType,
        BigDecimal strength
) {
}
