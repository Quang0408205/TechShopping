package com.example.Tech.dto.response.auth;

/** clientId is null while Google sign-in is switched off. */
public record GoogleConfigResponse(boolean enabled, String clientId) {
}
