package com.example.Tech.controller.promotion;

import com.example.Tech.dto.request.promotion.PromotionRequest;
import com.example.Tech.dto.request.promotion.PromotionSearchRequest;
import com.example.Tech.dto.response.common.ApiResult;
import com.example.Tech.dto.response.common.PageResponse;
import com.example.Tech.dto.response.promotion.PromotionResponse;
import com.example.Tech.service.promotion.AdminPromotionService;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Promotion management for administrators. Protected by URL (/api/v1/admin/** = ADMIN in SecurityConfig)
 * and by @PreAuthorize; the service also re-checks the caller's ADMIN role in the database.
 */
@RestController
@RequestMapping("/api/v1/admin/promotions")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Promotions", description = "Promotion management (ADMIN only)")
@ApiResponses({
        @ApiResponse(responseCode = "401", description = "UNAUTHORIZED, INVALID_TOKEN"),
        @ApiResponse(responseCode = "403", description = "ACCESS_DENIED, ACCOUNT_DISABLED")
})
public class AdminPromotionController {

    private final AdminPromotionService adminPromotionService;

    @GetMapping
    @Operation(summary = "Search promotions, newest first by default",
            description = "Filters are optional and combined with AND. Sort example: sort=startDate,desc")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page of promotions with their products"),
            @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR / MALFORMED_REQUEST")
    })
    public ResponseEntity<ApiResult<PageResponse<PromotionResponse>>> search(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @Valid @ParameterObject PromotionSearchRequest filter,
            @ParameterObject @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResult.ok(adminPromotionService.search(adminId(jwt), filter, pageable)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a promotion")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Promotion found"),
            @ApiResponse(responseCode = "404", description = "PROMOTION_NOT_FOUND")
    })
    public ResponseEntity<ApiResult<PromotionResponse>> getById(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                                                                @PathVariable Long id) {
        return ResponseEntity.ok(ApiResult.ok(adminPromotionService.getById(adminId(jwt), id)));
    }

    @PostMapping
    @Operation(summary = "Create a promotion with its products",
            description = "A product may not be in two active promotions whose periods overlap (end time exclusive)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Promotion created"),
            @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR, INVALID_PROMOTION_DATE_RANGE, "
                    + "INVALID_PROMOTION_DISCOUNT"),
            @ApiResponse(responseCode = "404", description = "PRODUCT_NOT_FOUND"),
            @ApiResponse(responseCode = "409", description = "PROMOTION_PRODUCT_OVERLAP")
    })
    public ResponseEntity<ApiResult<PromotionResponse>> create(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                                                               @Valid @RequestBody PromotionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResult.ok(adminPromotionService.create(adminId(jwt), request)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Replace a promotion and its product list")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Promotion updated"),
            @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR, INVALID_PROMOTION_DATE_RANGE, "
                    + "INVALID_PROMOTION_DISCOUNT"),
            @ApiResponse(responseCode = "404", description = "PROMOTION_NOT_FOUND, PRODUCT_NOT_FOUND"),
            @ApiResponse(responseCode = "409", description = "PROMOTION_PRODUCT_OVERLAP")
    })
    public ResponseEntity<ApiResult<PromotionResponse>> update(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                                                               @PathVariable Long id,
                                                               @Valid @RequestBody PromotionRequest request) {
        return ResponseEntity.ok(ApiResult.ok(adminPromotionService.update(adminId(jwt), id, request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a promotion (orders already placed keep their prices)")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Promotion deleted"),
            @ApiResponse(responseCode = "404", description = "PROMOTION_NOT_FOUND")
    })
    public ResponseEntity<Void> delete(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                                       @PathVariable Long id) {
        adminPromotionService.delete(adminId(jwt), id);
        return ResponseEntity.noContent().build();
    }

    /** The access token subject is the user id (see JwtTokenService). */
    private static Long adminId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
