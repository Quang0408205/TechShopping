package com.example.Tech.security;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtPropertiesTest {

    private static final String SECRET = "0123456789abcdef0123456789abcdef";

    @Test
    void validProperties_areAccepted() {
        JwtProperties properties = new JwtProperties(SECRET, "techshopping", Duration.ofMinutes(30), Duration.ofDays(7));

        assertThat(properties.secretBytes()).hasSize(JwtProperties.MIN_SECRET_BYTES);
    }

    @Test
    void missingSecret_failsWithClearMessage() {
        assertThatThrownBy(() -> new JwtProperties("", "techshopping", Duration.ofMinutes(30), Duration.ofDays(7)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET");
        assertThatThrownBy(() -> new JwtProperties(null, "techshopping", Duration.ofMinutes(30), Duration.ofDays(7)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void shortSecret_isRejected() {
        assertThatThrownBy(() -> new JwtProperties("too-short", "techshopping", Duration.ofMinutes(30), Duration.ofDays(7)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32 bytes");
    }

    @Test
    void missingIssuerOrTtl_isRejected() {
        assertThatThrownBy(() -> new JwtProperties(SECRET, " ", Duration.ofMinutes(30), Duration.ofDays(7)))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new JwtProperties(SECRET, "techshopping", Duration.ZERO, Duration.ofDays(7)))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new JwtProperties(SECRET, "techshopping", Duration.ofMinutes(30), null))
                .isInstanceOf(IllegalStateException.class);
    }
}
