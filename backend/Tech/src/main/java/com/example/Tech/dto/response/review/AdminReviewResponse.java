package com.example.Tech.dto.response.review;

import java.time.LocalDateTime;
import java.util.List;

/**
 * A review as the admin page sees it: author account, product, photos and the hide details.
 */
public record AdminReviewResponse(
        Long id,
        Long productId,
        String productName,
        Long authorId,
        String authorName,
        String authorUsername,
        String authorEmail,
        int rating,
        String comment,
        List<String> imageUrls,
        boolean verifiedPurchase,
        boolean edited,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        boolean hidden,
        String hiddenReason,
        String hiddenByName,
        LocalDateTime hiddenAt
) {
}
