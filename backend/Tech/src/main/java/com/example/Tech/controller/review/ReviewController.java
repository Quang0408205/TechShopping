package com.example.Tech.controller.review;

import com.example.Tech.dto.request.review.ReviewRequest;
import com.example.Tech.dto.response.common.ApiResult;
import com.example.Tech.dto.response.review.ReviewResponse;
import com.example.Tech.service.review.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * The caller's own reviews. Any logged-in account (the authenticated /api/v1/** rule); the account is re-checked in
 * the DB on every call. Another account's review id answers 404 REVIEW_NOT_FOUND.
 */
@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
@Tag(name = "Reviews", description = "Product reviews: public reads, the author's own writes")
@ApiResponses({
        @ApiResponse(responseCode = "401", description = "UNAUTHORIZED, INVALID_TOKEN"),
        @ApiResponse(responseCode = "403", description = "ACCOUNT_DISABLED")
})
public class ReviewController {

    private final ReviewService reviewService;

    @GetMapping("/mine")
    @Operation(summary = "My reviews, newest first (hidden ones too, with the reason)")
    @ApiResponse(responseCode = "200", description = "List (empty when none)")
    public ResponseEntity<ApiResult<List<ReviewResponse>>> mine(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "Only my review of this product") @RequestParam(required = false) Long productId) {
        return ResponseEntity.ok(ApiResult.ok(reviewService.mine(userId(jwt), productId)));
    }

    @PostMapping
    @Operation(summary = "Write a review (1–5 stars, 10–2000 characters, up to 5 photos); one per account and product")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Review created and shown at once"),
            @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR (details per field)"),
            @ApiResponse(responseCode = "404", description = "PRODUCT_NOT_FOUND"),
            @ApiResponse(responseCode = "409", description = "ALREADY_REVIEWED, PRODUCT_NOT_AVAILABLE")
    })
    public ResponseEntity<ApiResult<ReviewResponse>> create(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ReviewRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResult.ok(reviewService.create(userId(jwt), request)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Edit my review (stars, comment and photos are replaced; productId is ignored)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Review after the edit"),
            @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR"),
            @ApiResponse(responseCode = "404", description = "REVIEW_NOT_FOUND")
    })
    public ResponseEntity<ApiResult<ReviewResponse>> update(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id,
            @Valid @RequestBody ReviewRequest request) {
        return ResponseEntity.ok(ApiResult.ok(reviewService.update(userId(jwt), id, request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete my review (its photos too); the product can then be reviewed again")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Deleted"),
            @ApiResponse(responseCode = "404", description = "REVIEW_NOT_FOUND")
    })
    public ResponseEntity<Void> delete(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                                       @PathVariable Long id) {
        reviewService.delete(userId(jwt), id);
        return ResponseEntity.noContent().build();
    }

    /** The access token subject is the user id (see JwtTokenService). */
    private static Long userId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
