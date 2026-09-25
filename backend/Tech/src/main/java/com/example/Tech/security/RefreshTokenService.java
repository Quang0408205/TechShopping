package com.example.Tech.security;

import com.example.Tech.exception.BusinessException;
import com.example.Tech.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;

/**
 * Opaque refresh tokens stored in Redis (no database table). Only the SHA-256 hash of a token is used
 * as the key, so a Redis dump does not reveal usable tokens.
 * <ul>
 *     <li>{@code auth:refresh:<hash>} → user id, with the refresh TTL</li>
 *     <li>{@code auth:user-refresh:<userId>} → set of that user's token hashes, used by {@link #revokeAll(Long)};
 *     its TTL is extended on every issue, so it outlives the user's newest token</li>
 * </ul>
 * Rotation: the caller consumes the old token with {@link #consume(String)} (atomic GETDEL, so a token
 * works only once) and then issues a new one with {@link #issue(Long)}.
 */
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    static final String KEY_PREFIX = "auth:refresh:";
    static final String USER_INDEX_PREFIX = "auth:user-refresh:";
    private static final int TOKEN_BYTES = 32;

    private final StringRedisTemplate redisTemplate;
    private final JwtProperties properties;
    private final SecureRandom secureRandom = new SecureRandom();

    /** Creates a new refresh token for the user and returns its raw value. */
    public String issue(Long userId) {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        String hash = hash(token);

        redisTemplate.opsForValue().set(KEY_PREFIX + hash, String.valueOf(userId), properties.refreshTokenTtl());
        String indexKey = userIndexKey(userId);
        redisTemplate.opsForSet().add(indexKey, hash);
        redisTemplate.expire(indexKey, properties.refreshTokenTtl());
        return token;
    }

    /**
     * Invalidates the token and returns the id of its user.
     *
     * @throws BusinessException INVALID_TOKEN if the token is unknown, expired or already used
     */
    public Long consume(String token) {
        if (!StringUtils.hasText(token)) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }
        String hash = hash(token);
        String userId = redisTemplate.opsForValue().getAndDelete(KEY_PREFIX + hash);
        if (userId == null) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }
        redisTemplate.opsForSet().remove(USER_INDEX_PREFIX + userId, hash);
        return Long.valueOf(userId);
    }

    /** Deletes the token if it exists (logout). Unknown tokens are ignored. */
    public void revoke(String token) {
        if (!StringUtils.hasText(token)) {
            return;
        }
        String hash = hash(token);
        String userId = redisTemplate.opsForValue().getAndDelete(KEY_PREFIX + hash);
        if (userId != null) {
            redisTemplate.opsForSet().remove(USER_INDEX_PREFIX + userId, hash);
        }
    }

    /** Deletes every refresh token of the user (password change, deactivation, deletion). */
    public void revokeAll(Long userId) {
        String indexKey = userIndexKey(userId);
        Set<String> hashes = redisTemplate.opsForSet().members(indexKey);
        if (hashes != null && !hashes.isEmpty()) {
            List<String> tokenKeys = hashes.stream().map(hash -> KEY_PREFIX + hash).toList();
            redisTemplate.delete(tokenKeys);
        }
        redisTemplate.delete(indexKey);
    }

    static String key(String token) {
        return KEY_PREFIX + hash(token);
    }

    static String userIndexKey(Long userId) {
        return USER_INDEX_PREFIX + userId;
    }

    private static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }
}
