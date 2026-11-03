package com.example.Tech.controller.recommendation;

import com.example.Tech.dto.request.recommendation.RecommendationRequest;
import com.example.Tech.dto.response.common.ApiResult;
import com.example.Tech.dto.response.recommendation.RecommendationOverviewResponse;
import com.example.Tech.dto.response.recommendation.RecommendationResponse;
import com.example.Tech.service.recommendation.RecommendationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Product recommendations. Public: guests get popular products; with an access token the list is personal.
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Recommendations", description = "Personal, similar, upselling and cross-selling product lists (AI)")
public class RecommendationController {

    private final RecommendationService recommendationService;

    @PostMapping("/api/v1/recommendations")
    @Operation(summary = "Recommended products with a score and a reason")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List returned (source AI, or FALLBACK when the AI "
                    + "service is down)"),
            @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR"),
            @ApiResponse(responseCode = "401", description = "INVALID_TOKEN (a token was sent but is invalid)"),
            @ApiResponse(responseCode = "404", description = "PRODUCT_NOT_FOUND (fallback for a product page)")
    })
    public ResponseEntity<ApiResult<RecommendationResponse>> recommend(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody RecommendationRequest request) {
        Long userId = jwt != null ? Long.valueOf(jwt.getSubject()) : null;
        return ResponseEntity.ok(ApiResult.ok(recommendationService.recommend(userId, request)));
    }

    @PostMapping("/api/v1/admin/recommendations/train")
    @Operation(summary = "Retrain the recommendation models now (ADMIN; also runs every 24 hours)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Training statistics"),
            @ApiResponse(responseCode = "403", description = "FORBIDDEN"),
            @ApiResponse(responseCode = "503", description = "AI_SERVICE_UNAVAILABLE")
    })
    public ResponseEntity<ApiResult<Map<String, Object>>> train() {
        return ResponseEntity.ok(ApiResult.ok(recommendationService.train()));
    }

    @GetMapping("/api/v1/admin/recommendations/overview")
    @Operation(summary = "Model in use, latest offline evaluation and click-through rate of each list (ADMIN)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Overview (aiAvailable = false when the AI service is "
                    + "down: the online figures are still returned)"),
            @ApiResponse(responseCode = "403", description = "FORBIDDEN")
    })
    public ResponseEntity<ApiResult<RecommendationOverviewResponse>> overview(
            @Parameter(description = "Last N days of shown recommendations (kept within 1..365)")
            @RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok(ApiResult.ok(recommendationService.overview(days)));
    }

    @PostMapping("/api/v1/admin/recommendations/evaluation")
    @Operation(summary = "Run the offline evaluation again in the background (ADMIN); poll the overview for the result")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Evaluation status (running = true)"),
            @ApiResponse(responseCode = "403", description = "FORBIDDEN"),
            @ApiResponse(responseCode = "503", description = "AI_SERVICE_UNAVAILABLE")
    })
    public ResponseEntity<ApiResult<Map<String, Object>>> startEvaluation() {
        return ResponseEntity.ok(ApiResult.ok(recommendationService.startEvaluation()));
    }
}
