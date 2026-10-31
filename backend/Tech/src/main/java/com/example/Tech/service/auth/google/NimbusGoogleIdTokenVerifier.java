package com.example.Tech.service.auth.google;

import com.example.Tech.config.GoogleProperties;
import com.example.Tech.exception.BusinessException;
import com.example.Tech.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/** Verifies the RS256 signature against Google's published keys, then issuer, audience and expiry. */
@Slf4j
@Component
class NimbusGoogleIdTokenVerifier implements GoogleIdTokenVerifier {

    private static final String JWKS_URI = "https://www.googleapis.com/oauth2/v3/certs";
    private static final Set<String> ISSUERS = Set.of("https://accounts.google.com", "accounts.google.com");

    private final GoogleProperties properties;
    private final NimbusJwtDecoder decoder;

    NimbusGoogleIdTokenVerifier(GoogleProperties properties) {
        this.properties = properties;
        this.decoder = NimbusJwtDecoder.withJwkSetUri(JWKS_URI).build();
        this.decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                new JwtTimestampValidator(), issuerAndAudience()));
    }

    @Override
    public GoogleIdentity verify(String credential) {
        if (!properties.enabled()) {
            throw new BusinessException(ErrorCode.GOOGLE_LOGIN_UNAVAILABLE);
        }
        try {
            Jwt jwt = decoder.decode(credential);
            return new GoogleIdentity(jwt.getSubject(), jwt.getClaimAsString("email"),
                    Boolean.TRUE.equals(jwt.getClaim("email_verified")),
                    jwt.getClaimAsString("name"), jwt.getClaimAsString("picture"));
        } catch (JwtException e) {
            log.info("Google ID token rejected: {}", e.getMessage());
            throw new BusinessException(ErrorCode.GOOGLE_TOKEN_INVALID);
        }
    }

    private OAuth2TokenValidator<Jwt> issuerAndAudience() {
        return jwt -> {
            List<String> audience = jwt.getAudience();
            boolean issuerOk = jwt.getIssuer() != null && ISSUERS.contains(jwt.getIssuer().toString());
            boolean audienceOk = audience != null && properties.clientId() != null && audience.contains(properties.clientId());
            return issuerOk && audienceOk
                    ? OAuth2TokenValidatorResult.success()
                    : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Wrong issuer or audience", null));
        };
    }
}
