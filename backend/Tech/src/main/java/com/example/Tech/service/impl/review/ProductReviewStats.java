package com.example.Tech.service.impl.review;

import com.example.Tech.dto.response.review.ReviewSummaryResponse;
import com.example.Tech.entity.product.Product;
import com.example.Tech.repository.review.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Figures of a product's visible reviews, and products.rating / total_reviews kept equal to them. No own
 * transaction: the review services call refresh() inside theirs, after flushing the change and while holding the
 * product row lock (ProductRepository.findAllByIdInForUpdate), so two writers cannot store a stale average.
 */
@Component
@RequiredArgsConstructor
public class ProductReviewStats {

    private final ReviewRepository reviewRepository;

    public ReviewSummaryResponse summary(Long productId) {
        long[] counts = countsByStars(productId);
        long total = 0;
        long points = 0;
        List<ReviewSummaryResponse.StarCount> stars = new ArrayList<>();
        for (int star = 5; star >= 1; star--) {
            total += counts[star];
            points += counts[star] * star;
            stars.add(new ReviewSummaryResponse.StarCount(star, counts[star]));
        }
        return new ReviewSummaryResponse(productId, average(points, total), total, stars,
                reviewRepository.countVisibleWithImages(productId));
    }

    /** Recomputes the product's rating (2 decimals, 0 when none) and review count from its visible reviews. */
    public void refresh(Product product) {
        long[] counts = countsByStars(product.getId());
        long total = 0;
        long points = 0;
        for (int star = 1; star <= 5; star++) {
            total += counts[star];
            points += counts[star] * star;
        }
        product.setRating(average(points, total));
        product.setTotalReviews((int) total);
    }

    /** Index = number of stars (1–5). */
    private long[] countsByStars(Long productId) {
        long[] counts = new long[6];
        for (Object[] row : reviewRepository.countVisibleByRating(productId)) {
            counts[((Number) row[0]).intValue()] = ((Number) row[1]).longValue();
        }
        return counts;
    }

    private static BigDecimal average(long points, long total) {
        return total == 0
                ? BigDecimal.ZERO.setScale(2)
                : BigDecimal.valueOf(points).divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
    }
}
