package com.example.Tech.controller.contact;

import com.example.Tech.dto.request.contact.ContactRequestCreateRequest;
import com.example.Tech.dto.response.common.ApiResult;
import com.example.Tech.dto.response.contact.ContactRequestCreatedResponse;
import com.example.Tech.service.contact.ContactRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Contact form (public, SecurityConfig permitAll). A logged-in sender is linked to their account.
 */
@RestController
@RequestMapping("/api/v1/contact-requests")
@RequiredArgsConstructor
@Tag(name = "Contact", description = "Contact form (public)")
public class ContactRequestController {

    private final ContactRequestService contactRequestService;

    @PostMapping
    @Operation(summary = "Send a message from the Contact page",
            description = "No login needed; with a token the message is linked to the account. "
                    + "Limited per client IP and per email in a time window (app.contact.rate-limit)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Saved (status NEW)"),
            @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR (details per field)"),
            @ApiResponse(responseCode = "429", description = "TOO_MANY_REQUESTS")
    })
    public ResponseEntity<ApiResult<ContactRequestCreatedResponse>> create(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ContactRequestCreateRequest request,
            HttpServletRequest httpRequest) {
        Long userId = jwt == null ? null : Long.valueOf(jwt.getSubject());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResult.ok(contactRequestService.create(userId, httpRequest.getRemoteAddr(), request)));
    }
}
