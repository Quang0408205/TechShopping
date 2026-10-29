package com.example.Tech.service.product;

import com.example.Tech.config.SearchProperties;
import com.example.Tech.exception.ErrorCode;
import com.example.Tech.exception.TooManyRequestsException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/** Fixed-window counter per client IP for the search suggestions ({@code ratelimit:suggest:<ip>}); Redis down → allowed. */
@Slf4j
@Component
@RequiredArgsConstructor
public class SearchRateLimiter {

    private final StringRedisTemplate redisTemplate;
    private final SearchProperties properties;

    public void acquire(String clientIp) {
        String key = "ratelimit:suggest:" + (clientIp == null ? "unknown" : clientIp);
        try {
            Long count = redisTemplate.opsForValue().increment(key);
            if (count != null && count == 1) {
                redisTemplate.expire(key, properties.suggestWindow());
            }
            if (count != null && count > properties.suggestPerIp()) {
                Long left = redisTemplate.getExpire(key);
                long seconds = left == null || left <= 0 ? properties.suggestWindow().toSeconds() : left;
                throw new TooManyRequestsException(ErrorCode.TOO_MANY_REQUESTS,
                        "Bạn tìm kiếm quá nhanh, vui lòng thử lại sau ít giây", seconds);
            }
        } catch (TooManyRequestsException e) {
            throw e;
        } catch (RuntimeException e) {
            log.warn("Search rate limit skipped: {}", e.toString());
        }
    }
}
