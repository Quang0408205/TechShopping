package com.example.Tech.service.mail;

import com.example.Tech.config.MailProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;

/**
 * Fixed-window counter per recipient ({@code mail:to:<sha256(address)>}, hashed so a Redis dump lists no addresses):
 * a stranger's address typed into many orders cannot be flooded. The first hit of a window sets its expiry.
 */
@Component
@RequiredArgsConstructor
public class MailRateLimiter {

    private static final String PREFIX = "mail:to:";

    private final StringRedisTemplate redisTemplate;
    private final MailProperties properties;

    /** Counts one mail for this address; false when the address is over its limit for the window. */
    public boolean tryAcquire(String address) {
        String key = PREFIX + sha256(address.trim().toLowerCase(Locale.ROOT));
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            redisTemplate.expire(key, properties.window());
        }
        return count == null || count <= properties.perRecipientLimit();
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
