package com.example.Tech.service.auth.google;

/** The verified claims of a Google ID token. */
public record GoogleIdentity(String subject, String email, boolean emailVerified, String name, String pictureUrl) {
}
