package com.example.Tech.controller.recommendation;

import com.example.Tech.dto.request.recommendation.PreferenceRequest;
import com.example.Tech.dto.response.common.ApiResult;
import com.example.Tech.dto.response.recommendation.PreferenceOptionResponse;
import com.example.Tech.dto.response.recommendation.PreferenceResponse;
import com.example.Tech.service.recommendation.PreferenceService;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Shopping-preference questionnaire for recommendations to a customer without any activity yet.
 * The options are public (guests answer too, kept in their browser); the answers of a customer need a token.
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Recommendations", description = "Personal, similar, upselling and cross-selling product lists (AI)")
public class PreferenceController {

    private final PreferenceService preferenceService;

    @GetMapping("/api/v1/recommendations/preference-options")
    @Operation(summary = "Questionnaire choices: categories on sale with their brands and needs")
    @ApiResponse(responseCode = "200", description = "Choices returned")
    public ResponseEntity<ApiResult<List<PreferenceOptionResponse>>> options() {
        return ResponseEntity.ok(ApiResult.ok(preferenceService.options()));
    }

    @GetMapping("/api/v1/users/me/preferences")
    @Operation(summary = "My questionnaire answers, and whether I already have activity (then no need to ask)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Answers returned (answered = false when none)"),
            @ApiResponse(responseCode = "401", description = "UNAUTHORIZED, INVALID_TOKEN")
    })
    public ResponseEntity<ApiResult<PreferenceResponse>> get(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ApiResult.ok(preferenceService.get(Long.valueOf(jwt.getSubject()))));
    }

    @PutMapping("/api/v1/users/me/preferences")
    @Operation(summary = "Save my questionnaire answers (replaces the previous answers)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Answers saved"),
            @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR (no category, unknown id, a need of a "
                    + "category not chosen)"),
            @ApiResponse(responseCode = "401", description = "UNAUTHORIZED, INVALID_TOKEN")
    })
    public ResponseEntity<ApiResult<PreferenceResponse>> save(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                                                              @Valid @RequestBody PreferenceRequest request) {
        return ResponseEntity.ok(ApiResult.ok(preferenceService.save(Long.valueOf(jwt.getSubject()), request)));
    }
}
