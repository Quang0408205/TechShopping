package com.example.Tech.service.review;

import com.example.Tech.dto.request.review.AdminReviewSearchRequest;
import com.example.Tech.dto.request.review.ReviewVisibilityRequest;
import com.example.Tech.dto.response.common.PageResponse;
import com.example.Tech.dto.response.review.AdminReviewResponse;
import org.springframework.data.domain.Pageable;

/**
 * Review moderation (ADMIN only, re-checked in the DB): every review, hidden ones included; hide with a reason or
 * show again. Hiding / showing recomputes the product's rating (hidden reviews do not count).
 */
public interface AdminReviewService {

    PageResponse<AdminReviewResponse> search(Long adminId, AdminReviewSearchRequest filter, Pageable pageable);

    AdminReviewResponse setVisibility(Long adminId, Long reviewId, ReviewVisibilityRequest request);
}
