package com.example.Tech.dto.response.review;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

/**
 * One review as customers see it. Public lists only contain visible reviews (hidden = false, hiddenReason null);
 * GET /reviews/mine also returns the caller's hidden ones with the reason.
 */
public record ReviewResponse(
        Long id,
        Long productId,
        String productName,
        String authorName,
        int rating,
        String comment,
        List<String> imageUrls,

        @Schema(description = "The author has a DELIVERED order containing this product (computed when read)")
        boolean verifiedPurchase,

        @Schema(description = "The author edited the review after writing it")
        boolean edited,

        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        boolean hidden,
        String hiddenReason
) {
}
