package com.example.Tech.service.auth;

import com.example.Tech.config.AuthRateLimitProperties;
import com.example.Tech.exception.ErrorCode;
import com.example.Tech.exception.TooManyRequestsException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/** Fixed-window counters per client IP for register and token refresh ({@code ratelimit:<name>:<ip>}). */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthRequestRateLimiter {

    private final StringRedisTemplate redisTemplate;
    private final AuthRateLimitProperties properties;

    public void acquireRegister(String clientIp) {
        acquire("register", clientIp, properties.registerPerIp(), properties.registerWindow());
    }

    public void acquireRefresh(String clientIp) {
        acquire("refresh", clientIp, properties.refreshPerIp(), properties.refreshWindow());
    }

    private void acquire(String name, String clientIp, int limit, Duration window) {
        String key = "ratelimit:" + name + ":" + (clientIp == null ? "unknown" : clientIp);
        try {
            Long count = redisTemplate.opsForValue().increment(key);
            if (count != null && count == 1) {
                redisTemplate.expire(key, window);
            }
            if (count != null && count > limit) {
                Long left = redisTemplate.getExpire(key);
                long seconds = left == null || left <= 0 ? window.toSeconds() : left;
                long minutes = Duration.ofSeconds(seconds).toMinutes() + 1;
                throw new TooManyRequestsException(ErrorCode.TOO_MANY_REQUESTS,
                        "Bạn thao tác quá nhiều lần, vui lòng thử lại sau khoảng %d phút".formatted(minutes), seconds);
            }
        } catch (TooManyRequestsException e) {
            throw e;
        } catch (RuntimeException e) {
            log.warn("Rate limit skipped for {}: {}", name, e.toString());
        }
    }
}
