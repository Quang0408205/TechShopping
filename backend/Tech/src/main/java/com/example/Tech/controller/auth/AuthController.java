package com.example.Tech.controller.auth;

import com.example.Tech.config.GoogleProperties;
import com.example.Tech.dto.request.auth.ForgotPasswordRequest;
import com.example.Tech.dto.request.auth.GoogleLoginRequest;
import com.example.Tech.dto.request.auth.LoginRequest;
import com.example.Tech.dto.request.auth.RefreshTokenRequest;
import com.example.Tech.dto.request.auth.RegisterRequest;
import com.example.Tech.dto.request.auth.ResetPasswordRequest;
import com.example.Tech.dto.response.auth.AuthResponse;
import com.example.Tech.dto.response.auth.GoogleConfigResponse;
import com.example.Tech.dto.response.common.ApiResult;
import com.example.Tech.service.auth.AuthRequestRateLimiter;
import com.example.Tech.service.auth.AuthService;
import com.example.Tech.service.auth.PasswordResetService;
import com.example.Tech.service.auth.google.GoogleIdTokenVerifier;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Registration, login and tokens")
public class AuthController {

    private final AuthService authService;
    private final AuthRequestRateLimiter requestRateLimiter;
    private final PasswordResetService passwordResetService;
    private final GoogleIdTokenVerifier googleVerifier;
    private final GoogleProperties googleProperties;

    @PostMapping("/register")
    @Operation(summary = "Register a customer account and log in")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Account created; tokens returned"),
            @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR"),
            @ApiResponse(responseCode = "409", description = "DUPLICATE_EMAIL, DUPLICATE_USERNAME"),
            @ApiResponse(responseCode = "429", description = "TOO_MANY_REQUESTS (per client IP)")
    })
    public ResponseEntity<ApiResult<AuthResponse>> register(@Valid @RequestBody RegisterRequest request,
                                                              HttpServletRequest httpRequest) {
        requestRateLimiter.acquireRegister(httpRequest.getRemoteAddr());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResult.ok(authService.register(request)));
    }

    @PostMapping("/login")
    @Operation(summary = "Log in with email or username")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tokens returned"),
            @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR"),
            @ApiResponse(responseCode = "401", description = "INVALID_CREDENTIALS"),
            @ApiResponse(responseCode = "403", description = "ACCOUNT_DISABLED"),
            @ApiResponse(responseCode = "429", description = "TOO_MANY_LOGIN_ATTEMPTS (5 wrong passwords lock the account 15 minutes; Retry-After header)")
    })
    public ResponseEntity<ApiResult<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResult.ok(authService.login(request)));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Get a new token pair; the refresh token sent becomes invalid")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "New tokens returned"),
            @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR"),
            @ApiResponse(responseCode = "401", description = "INVALID_TOKEN"),
            @ApiResponse(responseCode = "403", description = "ACCOUNT_DISABLED"),
            @ApiResponse(responseCode = "429", description = "TOO_MANY_REQUESTS (per client IP)")
    })
    public ResponseEntity<ApiResult<AuthResponse>> refresh(@Valid @RequestBody RefreshTokenRequest request,
                                                             HttpServletRequest httpRequest) {
        requestRateLimiter.acquireRefresh(httpRequest.getRemoteAddr());
        return ResponseEntity.ok(ApiResult.ok(authService.refresh(request)));
    }

    @PostMapping("/logout")
    @Operation(summary = "Revoke a refresh token (the access token simply expires)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Logged out"),
            @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR")
    })
    public ResponseEntity<ApiResult<Void>> logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request);
        return ResponseEntity.ok(ApiResult.ok(null));
    }

    @GetMapping("/google/config")
    @Operation(summary = "Whether Google sign-in is on, and the client id the button needs")
    public ResponseEntity<ApiResult<GoogleConfigResponse>> googleConfig() {
        return ResponseEntity.ok(ApiResult.ok(new GoogleConfigResponse(googleProperties.enabled(), googleProperties.clientId())));
    }

    @PostMapping("/google")
    @Operation(summary = "Sign in (or register) with a Google ID token")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tokens returned"),
            @ApiResponse(responseCode = "400", description = "GOOGLE_EMAIL_NOT_VERIFIED"),
            @ApiResponse(responseCode = "401", description = "GOOGLE_TOKEN_INVALID"),
            @ApiResponse(responseCode = "403", description = "GOOGLE_LOGIN_NOT_ALLOWED (staff), ACCOUNT_DISABLED"),
            @ApiResponse(responseCode = "429", description = "TOO_MANY_REQUESTS (per client IP)"),
            @ApiResponse(responseCode = "503", description = "GOOGLE_LOGIN_UNAVAILABLE (no client id configured)")
    })
    public ResponseEntity<ApiResult<AuthResponse>> google(@Valid @RequestBody GoogleLoginRequest request,
                                                            HttpServletRequest httpRequest) {
        requestRateLimiter.acquireGoogle(httpRequest.getRemoteAddr());
        return ResponseEntity.ok(ApiResult.ok(authService.loginWithGoogle(googleVerifier.verify(request.credential()))));
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Email a link to reset the password; the answer is the same whether or not the email exists")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Accepted"),
            @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR"),
            @ApiResponse(responseCode = "429", description = "TOO_MANY_REQUESTS (per client IP)")
    })
    public ResponseEntity<ApiResult<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request,
                                                            HttpServletRequest httpRequest) {
        requestRateLimiter.acquirePasswordReset(httpRequest.getRemoteAddr());
        passwordResetService.request(request);
        return ResponseEntity.ok(ApiResult.ok(null));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Set a new password with the token from the email (single use, 30 minutes)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Password changed; every session of the account ends"),
            @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR, PASSWORD_RESET_TOKEN_INVALID"),
            @ApiResponse(responseCode = "429", description = "TOO_MANY_REQUESTS (per client IP)")
    })
    public ResponseEntity<ApiResult<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request,
                                                           HttpServletRequest httpRequest) {
        requestRateLimiter.acquirePasswordReset(httpRequest.getRemoteAddr());
        passwordResetService.reset(request);
        return ResponseEntity.ok(ApiResult.ok(null));
    }
}
