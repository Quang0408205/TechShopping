package com.example.Tech.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * JWT settings (app.jwt.*). The secret comes from the JWT_SECRET environment variable and is
 * validated at startup, so a missing or weak secret stops the application with a clear message.
 */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
        String secret,
        String issuer,
        Duration accessTokenTtl,
        Duration refreshTokenTtl
) {

    /** HS256 needs a key of at least 256 bits. */
    public static final int MIN_SECRET_BYTES = 32;

    public JwtProperties {
        if (!StringUtils.hasText(secret) || secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalStateException("app.jwt.secret (environment variable JWT_SECRET) must be set and at least "
                    + MIN_SECRET_BYTES + " bytes long");
        }
        if (!StringUtils.hasText(issuer)) {
            throw new IllegalStateException("app.jwt.issuer must be set");
        }
        if (accessTokenTtl == null || accessTokenTtl.isNegative() || accessTokenTtl.isZero()
                || refreshTokenTtl == null || refreshTokenTtl.isNegative() || refreshTokenTtl.isZero()) {
            throw new IllegalStateException("app.jwt.access-token-ttl and app.jwt.refresh-token-ttl must be positive");
        }
    }

    public byte[] secretBytes() {
        return secret.getBytes(StandardCharsets.UTF_8);
    }
}
