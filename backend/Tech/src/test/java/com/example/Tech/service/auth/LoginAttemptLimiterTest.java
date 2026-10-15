package com.example.Tech.service.auth;

import com.example.Tech.config.AuthRateLimitProperties;
import com.example.Tech.exception.ErrorCode;
import com.example.Tech.exception.TooManyRequestsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoginAttemptLimiterTest {

    private static final Duration QUARTER_HOUR = Duration.ofMinutes(15);

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private LoginAttemptLimiter limiter;

    @BeforeEach
    void setUp() {
        limiter = new LoginAttemptLimiter(redisTemplate,
                new AuthRateLimitProperties(5, QUARTER_HOUR, QUARTER_HOUR, 10, Duration.ofHours(1), 60,
                        Duration.ofMinutes(1)));
    }

    @Test
    void firstFailure_startsTheWindow() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment("auth:login-fail:u7")).thenReturn(1L);

        limiter.recordFailure("u7");

        verify(redisTemplate).expire("auth:login-fail:u7", QUARTER_HOUR);
        verify(valueOperations, never()).set(anyString(), anyString(), any(Duration.class));
    }

    @Test
    void fifthFailure_locksAndThrows429() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment("auth:login-fail:u7")).thenReturn(5L);

        assertThatThrownBy(() -> limiter.recordFailure("u7"))
                .isInstanceOfSatisfying(TooManyRequestsException.class, error -> {
                    assertThat(error.getErrorCode()).isEqualTo(ErrorCode.TOO_MANY_LOGIN_ATTEMPTS);
                    assertThat(error.getRetryAfterSeconds()).isEqualTo(900);
                    assertThat(error.getMessage()).contains("5 lần").contains("15 phút");
                });
        verify(valueOperations).set("auth:login-lock:u7", "1", QUARTER_HOUR);
        verify(redisTemplate).delete("auth:login-fail:u7");
    }

    @Test
    void lockedAccount_isRefused_withMinutesLeft() {
        when(redisTemplate.getExpire("auth:login-lock:u7")).thenReturn(125L);

        assertThatThrownBy(() -> limiter.ensureNotLocked("u7"))
                .isInstanceOfSatisfying(TooManyRequestsException.class, error -> {
                    assertThat(error.getRetryAfterSeconds()).isEqualTo(125);
                    assertThat(error.getMessage()).contains("3 phút");
                });
    }

    @Test
    void noLock_passes() {
        when(redisTemplate.getExpire("auth:login-lock:u7")).thenReturn(-2L);

        assertThatCode(() -> limiter.ensureNotLocked("u7")).doesNotThrowAnyException();
    }

    @Test
    void redisDown_letsTheLoginThrough() {
        when(redisTemplate.getExpire(anyString())).thenThrow(new RedisConnectionFailureException("down"));
        when(redisTemplate.opsForValue()).thenThrow(new RedisConnectionFailureException("down"));

        assertThatCode(() -> {
            limiter.ensureNotLocked("u7");
            limiter.recordFailure("u7");
            limiter.reset("u7");
        }).doesNotThrowAnyException();
    }

    @Test
    void unknownLogins_shareNoSubjectWithUsers() {
        assertThat(LoginAttemptLimiter.subjectOfUnknown("nobody")).startsWith("x").hasSize(65);
        assertThat(LoginAttemptLimiter.subjectOfUser(7L)).isEqualTo("u7");
    }
}
