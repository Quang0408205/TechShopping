package com.example.Tech.service.contact;

import com.example.Tech.config.ContactProperties;
import com.example.Tech.exception.BusinessException;
import com.example.Tech.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Locale;

/**
 * Fixed-window counters in Redis: {@code contact:ip:<ip>} and {@code contact:email:<sha256(email)>} (the email is
 * hashed so a Redis dump does not list addresses). The first hit of a window sets its expiry.
 */
@Component
@RequiredArgsConstructor
public class ContactRateLimiter {

    static final String IP_PREFIX = "contact:ip:";
    static final String EMAIL_PREFIX = "contact:email:";

    private final StringRedisTemplate redisTemplate;
    private final ContactProperties properties;

    /** Counts one send; 429 TOO_MANY_REQUESTS when the IP or the email is over its limit for this window. */
    public void acquire(String clientIp, String email) {
        check(IP_PREFIX + (clientIp == null ? "unknown" : clientIp), properties.perIp());
        check(EMAIL_PREFIX + sha256(email.trim().toLowerCase(Locale.ROOT)), properties.perEmail());
    }

    private void check(String key, int limit) {
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            redisTemplate.expire(key, properties.window());
        }
        if (count != null && count > limit) {
            Long seconds = redisTemplate.getExpire(key);
            long minutes = seconds == null || seconds <= 0 ? properties.window().toMinutes()
                    : Duration.ofSeconds(seconds).toMinutes() + 1;
            throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS,
                    "Bạn đã gửi nhiều tin nhắn liên tiếp, vui lòng thử lại sau khoảng %d phút".formatted(minutes));
        }
    }

    private static String sha256(String text) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
