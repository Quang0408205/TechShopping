package com.example.Tech.service.auth;

import com.example.Tech.dto.request.auth.LoginRequest;
import com.example.Tech.dto.request.auth.RefreshTokenRequest;
import com.example.Tech.dto.request.auth.RegisterRequest;
import com.example.Tech.dto.response.auth.AuthResponse;
import com.example.Tech.service.auth.google.GoogleIdentity;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    /** Rotates the refresh token: the given one becomes invalid and a new pair is returned. */
    AuthResponse refresh(RefreshTokenRequest request);

    void logout(RefreshTokenRequest request);

    /** Signs in with a verified Google identity: links it to the account with the same email, or creates a customer. */
    AuthResponse loginWithGoogle(GoogleIdentity identity);
}
