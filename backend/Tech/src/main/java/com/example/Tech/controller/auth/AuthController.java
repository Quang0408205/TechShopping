package com.example.Tech.controller.auth;

import com.example.Tech.dto.request.auth.LoginRequest;
import com.example.Tech.dto.request.auth.RefreshTokenRequest;
import com.example.Tech.dto.request.auth.RegisterRequest;
import com.example.Tech.dto.response.auth.AuthResponse;
import com.example.Tech.dto.response.common.ApiResult;
import com.example.Tech.service.auth.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    @PostMapping("/register")
    @Operation(summary = "Register a customer account and log in")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Account created; tokens returned"),
            @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR"),
            @ApiResponse(responseCode = "409", description = "DUPLICATE_EMAIL, DUPLICATE_USERNAME")
    })
    public ResponseEntity<ApiResult<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResult.ok(authService.register(request)));
    }

    @PostMapping("/login")
    @Operation(summary = "Log in with email or username")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tokens returned"),
            @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR"),
            @ApiResponse(responseCode = "401", description = "INVALID_CREDENTIALS"),
            @ApiResponse(responseCode = "403", description = "ACCOUNT_DISABLED")
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
            @ApiResponse(responseCode = "403", description = "ACCOUNT_DISABLED")
    })
    public ResponseEntity<ApiResult<AuthResponse>> refresh(@Valid @RequestBody RefreshTokenRequest request) {
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
}
