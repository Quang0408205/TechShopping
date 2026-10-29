package com.example.Tech.dto.response.review;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "A recent 4-5 star review shown on the home page (public)")
public record ReviewHighlightResponse(
        Long id,
        Long productId,
        String productName,
        @Schema(description = "Best image of the product; null when it has none") String productImageUrl,
        String authorName,
        int rating,
        String comment,
        @Schema(description = "The variant the author bought; null when unknown") String variantLabel,
        @Schema(description = "The author has a DELIVERED order containing this product") boolean verifiedPurchase,
        LocalDateTime createdAt
) {
}
