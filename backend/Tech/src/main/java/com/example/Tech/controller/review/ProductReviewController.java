package com.example.Tech.controller.review;

import com.example.Tech.dto.request.review.ReviewSearchRequest;
import com.example.Tech.dto.response.common.ApiResult;
import com.example.Tech.dto.response.common.PageResponse;
import com.example.Tech.dto.response.review.ReviewResponse;
import com.example.Tech.dto.response.review.ReviewSummaryResponse;
import com.example.Tech.service.review.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public review reads of a product (GET /api/v1/products/** is permitAll in SecurityConfig). Only visible reviews.
 */
@RestController
@RequestMapping("/api/v1/products/{productId}/reviews")
@RequiredArgsConstructor
@Tag(name = "Reviews", description = "Product reviews: public reads, the author's own writes")
@ApiResponse(responseCode = "404", description = "PRODUCT_NOT_FOUND (missing or deleted product)")
public class ProductReviewController {

    private final ReviewService reviewService;

    @GetMapping
    @Operation(summary = "Visible reviews of the product, newest first",
            description = "verifiedPurchase = the author has a DELIVERED order with this product")
    @ApiResponse(responseCode = "200", description = "Page of reviews")
    public ResponseEntity<ApiResult<PageResponse<ReviewResponse>>> list(
            @PathVariable Long productId,
            @Valid @ParameterObject ReviewSearchRequest filter,
            @ParameterObject @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(ApiResult.ok(reviewService.listForProduct(productId, filter, pageable)));
    }

    @GetMapping("/summary")
    @Operation(summary = "Average, total, count per star level (5 → 1) and with photos, of the visible reviews")
    @ApiResponse(responseCode = "200", description = "Real figures (the crawled TGDĐ rating is on the product)")
    public ResponseEntity<ApiResult<ReviewSummaryResponse>> summary(@PathVariable Long productId) {
        return ResponseEntity.ok(ApiResult.ok(reviewService.summary(productId)));
    }
}
