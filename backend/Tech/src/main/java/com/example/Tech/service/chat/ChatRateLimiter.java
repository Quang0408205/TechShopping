package com.example.Tech.service.chat;

import com.example.Tech.exception.BusinessException;
import com.example.Tech.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Every chatbot message costs an LLM call, so it is limited per hour: per account for logged-in customers,
 * per client IP for guests (fixed-window counters in Redis, like ContactRateLimiter).
 */
@Component
@RequiredArgsConstructor
public class ChatRateLimiter {

    static final int PER_USER = 60;
    static final int PER_GUEST_IP = 20;
    static final Duration WINDOW = Duration.ofHours(1);

    private final StringRedisTemplate redisTemplate;

    public void acquire(Long userId, String clientIp) {
        String key = userId != null ? "chat:user:" + userId : "chat:ip:" + (clientIp == null ? "unknown" : clientIp);
        int limit = userId != null ? PER_USER : PER_GUEST_IP;
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            redisTemplate.expire(key, WINDOW);
        }
        if (count != null && count > limit) {
            throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS,
                    userId != null ? "Bạn đã hỏi trợ lý quá nhiều lần trong một giờ, vui lòng thử lại sau"
                            : "Bạn đã hỏi trợ lý quá nhiều lần, hãy đăng nhập hoặc thử lại sau");
        }
    }
}
