package com.example.Tech.service.auth;

import com.example.Tech.config.AuthRateLimitProperties;
import com.example.Tech.exception.ErrorCode;
import com.example.Tech.exception.TooManyRequestsException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Wrong-password counter per account ({@code auth:login-fail:<subject>}) and the lock it triggers
 * ({@code auth:login-lock:<subject>}). The subject is the user id, or a hash of the typed login when no account
 * matches, so the response never reveals which logins exist. Redis errors let the login through (logged only).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LoginAttemptLimiter {

    static final String FAIL_PREFIX = "auth:login-fail:";
    static final String LOCK_PREFIX = "auth:login-lock:";

    private final StringRedisTemplate redisTemplate;
    private final AuthRateLimitProperties properties;

    public static String subjectOfUser(Long userId) {
        return "u" + userId;
    }

    public static String subjectOfUnknown(String normalizedIdentifier) {
        return "x" + sha256(normalizedIdentifier);
    }

    /** 429 while the account is locked, even when the password is right. */
    public void ensureNotLocked(String subject) {
        try {
            Long seconds = redisTemplate.getExpire(LOCK_PREFIX + subject);
            if (seconds != null && seconds > 0) {
                throw locked(seconds);
            }
        } catch (TooManyRequestsException e) {
            throw e;
        } catch (RuntimeException e) {
            log.warn("Login lock check skipped: {}", e.toString());
        }
    }

    /** Counts a wrong password; the last allowed failure locks the account (429 right away). */
    public void recordFailure(String subject) {
        try {
            String failKey = FAIL_PREFIX + subject;
            Long count = redisTemplate.opsForValue().increment(failKey);
            if (count != null && count == 1) {
                redisTemplate.expire(failKey, properties.failureWindow());
            }
            if (count != null && count >= properties.maxLoginFailures()) {
                redisTemplate.opsForValue().set(LOCK_PREFIX + subject, "1", properties.lockDuration());
                redisTemplate.delete(failKey);
                throw locked(properties.lockDuration().toSeconds());
            }
        } catch (TooManyRequestsException e) {
            throw e;
        } catch (RuntimeException e) {
            log.warn("Login failure not counted: {}", e.toString());
        }
    }

    public void reset(String subject) {
        try {
            redisTemplate.delete(FAIL_PREFIX + subject);
        } catch (RuntimeException e) {
            log.warn("Login counter not cleared: {}", e.toString());
        }
    }

    private TooManyRequestsException locked(long seconds) {
        long minutes = (seconds + 59) / 60;
        return new TooManyRequestsException(ErrorCode.TOO_MANY_LOGIN_ATTEMPTS,
                "Bạn đã nhập sai mật khẩu %d lần. Vui lòng thử lại sau %d phút."
                        .formatted(properties.maxLoginFailures(), minutes), seconds);
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
