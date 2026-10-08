package com.example.Tech.service.review;

import com.example.Tech.dto.request.review.ReviewRequest;
import com.example.Tech.dto.request.review.ReviewSearchRequest;
import com.example.Tech.dto.response.common.PageResponse;
import com.example.Tech.dto.response.review.ReviewResponse;
import com.example.Tech.dto.response.review.ReviewSummaryResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Product reviews written by customers (any logged-in account): one per account and product, shown at once,
 * editable and deletable by their author. Every write recomputes products.rating / total_reviews.
 */
public interface ReviewService {

    /** Visible reviews of a product (public), newest first. */
    PageResponse<ReviewResponse> listForProduct(Long productId, ReviewSearchRequest filter, Pageable pageable);

    /** Real figures of the product's visible reviews (public). */
    ReviewSummaryResponse summary(Long productId);

    /** The caller's reviews (hidden ones included, with the reason), optionally of one product. */
    List<ReviewResponse> mine(Long userId, Long productId);

    ReviewResponse create(Long userId, ReviewRequest request);

    /** Edits the caller's review; a review hidden by an admin stays hidden. */
    ReviewResponse update(Long userId, Long reviewId, ReviewRequest request);

    void delete(Long userId, Long reviewId);
}
