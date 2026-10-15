package com.example.Tech.controller.recommendation;

import com.example.Tech.dto.request.recommendation.InteractionRequest;
import com.example.Tech.service.recommendation.InteractionRecorder;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Behaviour reported by the browser for the recommendation models. Requires a valid access token: guests
 * are not tracked (they get popular products, report table 3.21).
 */
@RestController
@RequestMapping("/api/v1/interactions")
@RequiredArgsConstructor
@Tag(name = "Interactions", description = "Viewing / clicking products, input of the recommendation system")
@ApiResponses({
        @ApiResponse(responseCode = "401", description = "UNAUTHORIZED, INVALID_TOKEN"),
        @ApiResponse(responseCode = "403", description = "ACCOUNT_DISABLED")
})
public class InteractionController {

    private final InteractionRecorder interactionRecorder;

    @PostMapping
    @Operation(summary = "Record that I viewed or clicked a product (repeats within 30 minutes are ignored)")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Recorded (or ignored as a repeat)"),
            @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR"),
            @ApiResponse(responseCode = "404", description = "PRODUCT_NOT_FOUND")
    })
    public ResponseEntity<Void> record(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                                       @Valid @RequestBody InteractionRequest request) {
        interactionRecorder.recordFromClient(Long.valueOf(jwt.getSubject()), request);
        return ResponseEntity.noContent().build();
    }
}
