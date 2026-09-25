package com.example.Tech.security;

import com.example.Tech.entity.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidationException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Uses the real JwtConfig beans (Nimbus encoder/decoder, HS256), without a Spring context.
 */
class JwtTokenServiceTest {

    private static final String SECRET = "unit-test-secret-0123456789abcdef-xyz";

    private final JwtConfig jwtConfig = new JwtConfig();
    private final JwtProperties properties = properties(SECRET, "techshopping");
    private final JwtDecoder decoder = jwtConfig.jwtDecoder(properties);

    private static JwtProperties properties(String secret, String issuer) {
        return new JwtProperties(secret, issuer, Duration.ofMinutes(30), Duration.ofDays(7));
    }

    private JwtTokenService service(JwtProperties props, Clock clock) {
        return new JwtTokenService(jwtConfig.jwtEncoder(props), props, clock);
    }

    private static User user() {
        User user = new User();
        user.setId(42L);
        user.setUsername("alice");
        return user;
    }

    @Test
    void issuedToken_containsUserClaimsAndExpiresAfter30Minutes() {
        String token = service(properties, Clock.systemUTC()).issueAccessToken(user(), List.of("CUSTOMER", "ADMIN"));

        Jwt jwt = decoder.decode(token);
        assertThat(jwt.getSubject()).isEqualTo("42");
        assertThat(jwt.getClaimAsString(JwtConfig.USERNAME_CLAIM)).isEqualTo("alice");
        assertThat(jwt.getClaimAsStringList(JwtConfig.ROLES_CLAIM)).containsExactly("CUSTOMER", "ADMIN");
        assertThat(jwt.getClaimAsString("iss")).isEqualTo("techshopping");
        assertThat(Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt())).isEqualTo(Duration.ofMinutes(30));
        assertThat(jwt.getHeaders()).containsEntry("alg", "HS256");
    }

    @Test
    void accessTokenTtlSeconds_matchesProperties() {
        assertThat(service(properties, Clock.systemUTC()).accessTokenTtlSeconds()).isEqualTo(1800);
    }

    @Test
    void expiredToken_isRejected() {
        Clock twoHoursAgo = Clock.fixed(Instant.now().minus(Duration.ofHours(2)), ZoneOffset.UTC);
        String token = service(properties, twoHoursAgo).issueAccessToken(user(), List.of("CUSTOMER"));

        assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtValidationException.class);
    }

    @Test
    void tokenSignedWithAnotherSecret_isRejected() {
        JwtProperties other = properties("another-secret-0123456789abcdef-xyz", "techshopping");
        String token = service(other, Clock.systemUTC()).issueAccessToken(user(), List.of("ADMIN"));

        assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(BadJwtException.class);
    }

    @Test
    void tokenWithAnotherIssuer_isRejected() {
        JwtProperties otherIssuer = properties(SECRET, "someone-else");
        String token = service(otherIssuer, Clock.systemUTC()).issueAccessToken(user(), List.of("ADMIN"));

        assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtValidationException.class);
    }

    @Test
    void rolesClaim_becomesRoleAuthorities() {
        String token = service(properties, Clock.systemUTC()).issueAccessToken(user(), List.of("CUSTOMER", "ADMIN"));

        var authentication = jwtConfig.jwtAuthenticationConverter().convert(decoder.decode(token));

        // Spring Security 7 also adds a FACTOR_BEARER authority (multi-factor support); only roles matter here
        assertThat(authentication.getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .filteredOn(authority -> authority.startsWith("ROLE_"))
                .containsExactlyInAnyOrder("ROLE_CUSTOMER", "ROLE_ADMIN");
        assertThat(authentication.getName()).isEqualTo("42");
    }
}
