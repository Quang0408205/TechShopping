package com.example.Tech.controller.user;

import com.example.Tech.dto.request.user.UserRolesUpdateRequest;
import com.example.Tech.dto.request.user.UserSearchRequest;
import com.example.Tech.dto.request.user.UserStatusUpdateRequest;
import com.example.Tech.dto.response.common.ApiResult;
import com.example.Tech.dto.response.common.PageResponse;
import com.example.Tech.dto.response.user.UserResponse;
import com.example.Tech.service.user.AdminUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * User management for administrators. Protected twice: by URL (/api/v1/admin/** in SecurityConfig)
 * and by @PreAuthorize; the service also re-checks the caller's ADMIN role in the database.
 */
@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Users", description = "User management (ADMIN only)")
@ApiResponses({
        @ApiResponse(responseCode = "401", description = "UNAUTHORIZED, INVALID_TOKEN"),
        @ApiResponse(responseCode = "403", description = "ACCESS_DENIED, ACCOUNT_DISABLED")
})
public class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping
    @Operation(summary = "Search users with pagination (soft-deleted users only with includeDeleted=true)",
            description = "Filters are optional and combined with AND. Sort example: sort=createdAt,desc")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page of users returned"),
            @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR / MALFORMED_REQUEST")
    })
    public ResponseEntity<ApiResult<PageResponse<UserResponse>>> search(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @Valid @ParameterObject UserSearchRequest filter,
            @ParameterObject @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(ApiResult.ok(adminUserService.search(adminId(jwt), filter, pageable)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a user by id (soft-deleted users included)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User found"),
            @ApiResponse(responseCode = "404", description = "USER_NOT_FOUND")
    })
    public ResponseEntity<ApiResult<UserResponse>> getById(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                                                           @PathVariable Long id) {
        return ResponseEntity.ok(ApiResult.ok(adminUserService.getById(adminId(jwt), id)));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Activate or deactivate a user; deactivation logs out all of their sessions")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status updated"),
            @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR"),
            @ApiResponse(responseCode = "404", description = "USER_NOT_FOUND"),
            @ApiResponse(responseCode = "409", description = "CANNOT_MODIFY_OWN_ACCOUNT, LAST_ADMIN, USER_DELETED")
    })
    public ResponseEntity<ApiResult<UserResponse>> updateStatus(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                                                                @PathVariable Long id,
                                                                @Valid @RequestBody UserStatusUpdateRequest request) {
        return ResponseEntity.ok(ApiResult.ok(adminUserService.updateStatus(adminId(jwt), id, request)));
    }

    @PutMapping("/{id}/roles")
    @Operation(summary = "Replace the roles of a user (applied to their next access token)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Roles updated"),
            @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR"),
            @ApiResponse(responseCode = "404", description = "USER_NOT_FOUND, ROLE_NOT_FOUND"),
            @ApiResponse(responseCode = "409", description = "CANNOT_MODIFY_OWN_ACCOUNT, LAST_ADMIN, USER_DELETED")
    })
    public ResponseEntity<ApiResult<UserResponse>> updateRoles(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                                                               @PathVariable Long id,
                                                               @Valid @RequestBody UserRolesUpdateRequest request) {
        return ResponseEntity.ok(ApiResult.ok(adminUserService.updateRoles(adminId(jwt), id, request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft-delete a user and log out all of their sessions")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "User deleted (or was already deleted)"),
            @ApiResponse(responseCode = "404", description = "USER_NOT_FOUND"),
            @ApiResponse(responseCode = "409", description = "CANNOT_MODIFY_OWN_ACCOUNT, LAST_ADMIN")
    })
    public ResponseEntity<Void> delete(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                                       @PathVariable Long id) {
        adminUserService.delete(adminId(jwt), id);
        return ResponseEntity.noContent().build();
    }

    /** The access token subject is the user id (see JwtTokenService). */
    private static Long adminId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
