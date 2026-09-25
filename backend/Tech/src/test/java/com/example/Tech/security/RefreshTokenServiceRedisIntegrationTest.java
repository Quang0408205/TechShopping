package com.example.Tech.security;

import com.example.Tech.exception.BusinessException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Runs against the real Redis container (docker compose). Every token created here is consumed or
 * revoked, so no keys are left behind.
 */
@SpringBootTest
@ActiveProfiles("test")
class RefreshTokenServiceRedisIntegrationTest {

    private static final Long USER_ID = 900001L;
    private static final Long OTHER_USER_ID = 900002L;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @AfterEach
    void cleanUp() {
        refreshTokenService.revokeAll(USER_ID);
    }

    @Test
    void issuedToken_isStoredWithTtl_andCanBeConsumedOnlyOnce() {
        String token = refreshTokenService.issue(USER_ID);
        String key = RefreshTokenService.key(token);
        String indexKey = RefreshTokenService.userIndexKey(USER_ID);

        assertThat(redisTemplate.opsForValue().get(key)).isEqualTo(String.valueOf(USER_ID));
        Long ttlSeconds = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        assertThat(ttlSeconds).isBetween(Duration.ofDays(7).minusMinutes(1).toSeconds(), Duration.ofDays(7).toSeconds());
        assertThat(redisTemplate.opsForSet().size(indexKey)).isEqualTo(1);
        assertThat(redisTemplate.getExpire(indexKey, TimeUnit.SECONDS)).isPositive();

        assertThat(refreshTokenService.consume(token)).isEqualTo(USER_ID);
        assertThat(redisTemplate.hasKey(key)).isFalse();
        assertThat(redisTemplate.hasKey(indexKey)).isFalse();
        assertThatThrownBy(() -> refreshTokenService.consume(token)).isInstanceOf(BusinessException.class);
    }

    @Test
    void revokedToken_cannotBeConsumed() {
        String token = refreshTokenService.issue(USER_ID);

        refreshTokenService.revoke(token);

        assertThat(redisTemplate.hasKey(RefreshTokenService.key(token))).isFalse();
        assertThatThrownBy(() -> refreshTokenService.consume(token)).isInstanceOf(BusinessException.class);
    }

    @Test
    void revokeAll_invalidatesEveryTokenOfTheUser_only() {
        String first = refreshTokenService.issue(USER_ID);
        String second = refreshTokenService.issue(USER_ID);
        String otherUsers = refreshTokenService.issue(OTHER_USER_ID);

        refreshTokenService.revokeAll(USER_ID);

        assertThatThrownBy(() -> refreshTokenService.consume(first)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> refreshTokenService.consume(second)).isInstanceOf(BusinessException.class);
        assertThat(redisTemplate.hasKey(RefreshTokenService.userIndexKey(USER_ID))).isFalse();
        assertThat(refreshTokenService.consume(otherUsers)).isEqualTo(OTHER_USER_ID);
    }
}
