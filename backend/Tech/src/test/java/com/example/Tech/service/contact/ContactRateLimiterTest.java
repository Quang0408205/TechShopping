package com.example.Tech.service.contact;

import com.example.Tech.config.ContactProperties;
import com.example.Tech.exception.BusinessException;
import com.example.Tech.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContactRateLimiterTest {

    private static final Duration HOUR = Duration.ofHours(1);

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private ContactRateLimiter limiter;

    @BeforeEach
    void setUp() {
        limiter = new ContactRateLimiter(redisTemplate, new ContactProperties(2, 3, HOUR));
    }

    @Test
    void firstHit_setsTheWindow_andEmailIsHashed() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenReturn(1L);

        limiter.acquire("10.0.0.1", " Khach@Example.com ");

        verify(redisTemplate).expire("contact:ip:10.0.0.1", HOUR);
        verify(valueOperations).increment(startsWith(ContactRateLimiter.EMAIL_PREFIX));
        verify(valueOperations, never()).increment(eq("contact:email:khach@example.com"));
    }

    @Test
    void overIpLimit_429_withMinutesLeft_emailNotCounted() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment("contact:ip:10.0.0.1")).thenReturn(3L);
        when(redisTemplate.getExpire("contact:ip:10.0.0.1")).thenReturn(600L);

        assertThatThrownBy(() -> limiter.acquire("10.0.0.1", "khach@example.com"))
                .isInstanceOf(BusinessException.class)
                .satisfies(error -> {
                    assertThat(((BusinessException) error).getErrorCode()).isEqualTo(ErrorCode.TOO_MANY_REQUESTS);
                    assertThat(error.getMessage()).contains("11 phút");
                });
        verify(valueOperations, never()).increment(startsWith(ContactRateLimiter.EMAIL_PREFIX));
        verify(redisTemplate, never()).expire(anyString(), eq(HOUR));
    }

    @Test
    void defaults_whenNotConfigured() {
        ContactProperties properties = new ContactProperties(null, 0, Duration.ZERO);
        assertThat(properties.perIp()).isEqualTo(10);
        assertThat(properties.perEmail()).isEqualTo(3);
        assertThat(properties.window()).isEqualTo(HOUR);
    }
}
