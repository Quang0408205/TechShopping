package com.example.Tech.dto.response.review;

import java.math.BigDecimal;
import java.util.List;

/**
 * Real figures of a product's visible reviews: average (2 decimals, 0 when none), total, the count for every star
 * level from 5 down to 1 (zeros included) and how many have photos. The crawled Thế Giới Di Động rating is not part
 * of it (ProductResponse.tgddRating).
 */
public record ReviewSummaryResponse(
        Long productId,
        BigDecimal averageRating,
        long totalReviews,
        List<StarCount> stars,
        long withImages
) {

    public record StarCount(int stars, long count) {
    }
}
