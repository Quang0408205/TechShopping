package com.example.Tech.controller.contact;

import com.example.Tech.dto.request.contact.ContactRequestSearchRequest;
import com.example.Tech.dto.request.contact.ContactRequestUpdateRequest;
import com.example.Tech.dto.response.common.ApiResult;
import com.example.Tech.dto.response.common.PageResponse;
import com.example.Tech.dto.response.contact.ContactRequestResponse;
import com.example.Tech.service.contact.ContactRequestService;
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
 * Contact messages for the shop: one shared inbox for STAFF / BRANCH_MANAGER of every store; ADMIN reads only.
 * Roles and the store assignment are re-checked in the DB by the service (StoreAccessGuard).
 */
@RestController
@RequestMapping("/api/v1/admin/contact-requests")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('STAFF', 'BRANCH_MANAGER', 'ADMIN')")
@Tag(name = "Admin - Contact requests", description = "Contact form messages (STAFF / BRANCH_MANAGER handle, ADMIN reads)")
@ApiResponses({
        @ApiResponse(responseCode = "401", description = "UNAUTHORIZED, INVALID_TOKEN"),
        @ApiResponse(responseCode = "403", description = "ACCESS_DENIED, ACCOUNT_DISABLED, NO_ACTIVE_STORE_ASSIGNMENT")
})
public class AdminContactRequestController {

    private final ContactRequestService contactRequestService;

    @GetMapping
    @Operation(summary = "Contact messages, newest first by default")
    @ApiResponse(responseCode = "200", description = "Page of messages")
    public ResponseEntity<ApiResult<PageResponse<ContactRequestResponse>>> search(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @Valid @ParameterObject ContactRequestSearchRequest filter,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return ResponseEntity.ok(ApiResult.ok(contactRequestService.search(userId(jwt), filter, pageable)));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Change the status and / or the internal note",
            description = "NEW → IN_PROGRESS / RESOLVED, IN_PROGRESS → RESOLVED, RESOLVED → IN_PROGRESS (reopen); "
                    + "never back to NEW. RESOLVED needs a note. ADMIN cannot change anything")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "The message after the change"),
            @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR (details.staffNote)"),
            @ApiResponse(responseCode = "403", description = "ADMIN_READ_ONLY"),
            @ApiResponse(responseCode = "404", description = "CONTACT_REQUEST_NOT_FOUND"),
            @ApiResponse(responseCode = "409", description = "INVALID_CONTACT_STATUS")
    })
    public ResponseEntity<ApiResult<ContactRequestResponse>> update(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id,
            @Valid @RequestBody ContactRequestUpdateRequest request) {
        return ResponseEntity.ok(ApiResult.ok(contactRequestService.update(userId(jwt), id, request)));
    }

    /** The access token subject is the user id (see JwtTokenService). */
    private static Long userId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
