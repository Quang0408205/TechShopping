package com.example.Tech.controller.user;

import com.example.Tech.dto.request.user.ChangePasswordRequest;
import com.example.Tech.dto.request.user.CustomerProfileUpdateRequest;
import com.example.Tech.dto.request.user.UserUpdateRequest;
import com.example.Tech.dto.response.common.ApiResult;
import com.example.Tech.dto.response.user.CustomerProfileResponse;
import com.example.Tech.dto.response.user.UserResponse;
import com.example.Tech.service.user.UserService;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The logged-in user's own account. Requires a valid access token (see SecurityConfig).
 */
@RestController
@RequestMapping("/api/v1/users/me")
@RequiredArgsConstructor
@Tag(name = "My account", description = "The logged-in user's own account and customer profile")
@ApiResponses({
        @ApiResponse(responseCode = "401", description = "UNAUTHORIZED, INVALID_TOKEN"),
        @ApiResponse(responseCode = "403", description = "ACCOUNT_DISABLED")
})
public class UserController {

    private final UserService userService;

    @GetMapping
    @Operation(summary = "Get my account")
    @ApiResponse(responseCode = "200", description = "Account returned")
    public ResponseEntity<ApiResult<UserResponse>> getMe(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ApiResult.ok(userService.getMe(userId(jwt))));
    }

    @PutMapping
    @Operation(summary = "Update my account (email and username cannot be changed)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Account updated"),
            @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR")
    })
    public ResponseEntity<ApiResult<UserResponse>> updateMe(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                                                            @Valid @RequestBody UserUpdateRequest request) {
        return ResponseEntity.ok(ApiResult.ok(userService.updateMe(userId(jwt), request)));
    }

    @PutMapping("/password")
    @Operation(summary = "Change my password; every session (refresh token) is logged out")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Password changed"),
            @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR, INVALID_PASSWORD")
    })
    public ResponseEntity<ApiResult<Void>> changePassword(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                                                          @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(userId(jwt), request);
        return ResponseEntity.ok(ApiResult.ok(null));
    }

    @GetMapping("/profile")
    @Operation(summary = "Get my customer profile")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile returned"),
            @ApiResponse(responseCode = "404", description = "CUSTOMER_PROFILE_NOT_FOUND")
    })
    public ResponseEntity<ApiResult<CustomerProfileResponse>> getMyProfile(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ApiResult.ok(userService.getMyProfile(userId(jwt))));
    }

    @PutMapping("/profile")
    @Operation(summary = "Create or update my customer profile")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile saved"),
            @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR")
    })
    public ResponseEntity<ApiResult<CustomerProfileResponse>> updateMyProfile(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CustomerProfileUpdateRequest request) {
        return ResponseEntity.ok(ApiResult.ok(userService.updateMyProfile(userId(jwt), request)));
    }

    /** The access token subject is the user id (see JwtTokenService). */
    private static Long userId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
