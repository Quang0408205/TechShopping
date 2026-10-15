package com.example.Tech.service.promotion;

import com.example.Tech.dto.request.promotion.PromotionRequest;
import com.example.Tech.dto.request.promotion.PromotionSearchRequest;
import com.example.Tech.dto.response.common.PageResponse;
import com.example.Tech.dto.response.promotion.PromotionResponse;
import org.springframework.data.domain.Pageable;

/**
 * Promotion management (ADMIN only). Every method re-checks the caller's ADMIN role in the database.
 */
public interface AdminPromotionService {

    PageResponse<PromotionResponse> search(Long adminId, PromotionSearchRequest filter, Pageable pageable);

    PromotionResponse getById(Long adminId, Long promotionId);

    PromotionResponse create(Long adminId, PromotionRequest request);

    PromotionResponse update(Long adminId, Long promotionId, PromotionRequest request);

    void delete(Long adminId, Long promotionId);
}
