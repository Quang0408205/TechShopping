package com.example.Tech.controller.review;

import com.example.Tech.dto.request.review.AdminReviewSearchRequest;
import com.example.Tech.dto.request.review.ReviewVisibilityRequest;
import com.example.Tech.dto.response.common.ApiResult;
import com.example.Tech.dto.response.common.PageResponse;
import com.example.Tech.dto.response.review.AdminReviewResponse;
import com.example.Tech.service.review.AdminReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Review moderation. ADMIN only: by URL (SecurityConfig, /api/v1/admin/**), @PreAuthorize, and the role re-checked
 * in the DB by the service.
 */
@RestController
@RequestMapping("/api/v1/admin/reviews")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Reviews", description = "Review moderation (ADMIN)")
@ApiResponses({
        @ApiResponse(responseCode = "401", description = "UNAUTHORIZED, INVALID_TOKEN"),
        @ApiResponse(responseCode = "403", description = "ACCESS_DENIED, ACCOUNT_DISABLED")
})
public class AdminReviewController {

    private final AdminReviewService adminReviewService;

    @GetMapping
    @Operation(summary = "Every review (hidden ones too), newest first by default")
    @ApiResponse(responseCode = "200", description = "Page of reviews")
    public ResponseEntity<ApiResult<PageResponse<AdminReviewResponse>>> search(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @Valid @ParameterObject AdminReviewSearchRequest filter,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return ResponseEntity.ok(ApiResult.ok(adminReviewService.search(userId(jwt), filter, pageable)));
    }

    @PatchMapping("/{id}/visibility")
    @Operation(summary = "Hide a review (reason required, shown to its author) or show it again",
            description = "The product's rating and review count are recomputed (hidden reviews do not count)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "The review after the change"),
            @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR (details.reason when hiding)"),
            @ApiResponse(responseCode = "404", description = "REVIEW_NOT_FOUND")
    })
    public ResponseEntity<ApiResult<AdminReviewResponse>> setVisibility(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id,
            @Valid @RequestBody ReviewVisibilityRequest request) {
        return ResponseEntity.ok(ApiResult.ok(adminReviewService.setVisibility(userId(jwt), id, request)));
    }

    /** The access token subject is the user id (see JwtTokenService). */
    private static Long userId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
