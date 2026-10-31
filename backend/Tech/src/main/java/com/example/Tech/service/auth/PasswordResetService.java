package com.example.Tech.service.auth;

import com.example.Tech.dto.request.auth.ForgotPasswordRequest;
import com.example.Tech.dto.request.auth.ResetPasswordRequest;
import com.example.Tech.entity.user.RoleName;
import com.example.Tech.entity.user.User;
import com.example.Tech.exception.BusinessException;
import com.example.Tech.exception.ErrorCode;
import com.example.Tech.repository.user.UserRepository;
import com.example.Tech.repository.user.UserRoleRepository;
import com.example.Tech.security.RefreshTokenService;
import com.example.Tech.service.mail.PasswordResetMailService;
import com.example.Tech.util.AccountUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

/**
 * Forgot / reset password for customer accounts. The link carries a random token and only its SHA-256 is kept in Redis:
 * {@code auth:password-reset:<hash>} holds the user id, {@code auth:password-reset-user:<id>} the account's current
 * hash, so a newer request replaces the older link. The answer to a request is the same whether or not the email
 * exists. Staff accounts get their passwords from a manager, so they are skipped.
 */
@Slf4j
@Service
public class PasswordResetService {

    public static final Duration TOKEN_TTL = Duration.ofMinutes(30);
    static final int MAX_REQUESTS_PER_EMAIL = 3;
    private static final Duration EMAIL_WINDOW = Duration.ofHours(1);

    private static final String TOKEN_PREFIX = "auth:password-reset:";
    private static final String USER_PREFIX = "auth:password-reset-user:";
    private static final String EMAIL_PREFIX = "auth:password-reset-email:";

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final StringRedisTemplate redisTemplate;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final LoginAttemptLimiter loginAttemptLimiter;
    private final PasswordResetMailService mailService;
    private final TaskExecutor mailExecutor;
    private final SecureRandom secureRandom = new SecureRandom();

    public PasswordResetService(UserRepository userRepository, UserRoleRepository userRoleRepository,
                                StringRedisTemplate redisTemplate, PasswordEncoder passwordEncoder,
                                RefreshTokenService refreshTokenService, LoginAttemptLimiter loginAttemptLimiter,
                                PasswordResetMailService mailService,
                                @Qualifier("mailExecutor") TaskExecutor mailExecutor) {
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
        this.redisTemplate = redisTemplate;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokenService = refreshTokenService;
        this.loginAttemptLimiter = loginAttemptLimiter;
        this.mailService = mailService;
        this.mailExecutor = mailExecutor;
    }

    public void request(ForgotPasswordRequest request) {
        String email = AccountUtil.normalize(request.email());
        Optional<User> found = userRepository.findByEmail(email);
        if (found.isEmpty() || !found.get().isEnabled() || !isCustomerOnly(found.get().getId())
                || overEmailLimit(email)) {
            return;
        }
        User user = found.get();
        String token = newToken();
        String hash = hash(token);
        String previous = redisTemplate.opsForValue().get(USER_PREFIX + user.getId());
        if (previous != null) {
            redisTemplate.delete(TOKEN_PREFIX + previous);
        }
        redisTemplate.opsForValue().set(TOKEN_PREFIX + hash, String.valueOf(user.getId()), TOKEN_TTL);
        redisTemplate.opsForValue().set(USER_PREFIX + user.getId(), hash, TOKEN_TTL);
        mailExecutor.execute(() -> mailService.send(user, token, TOKEN_TTL.toMinutes()));
    }

    @Transactional
    public void reset(ResetPasswordRequest request) {
        if (AccountUtil.exceedsBcryptLimit(request.newPassword())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "Password must be at most %d bytes".formatted(AccountUtil.MAX_PASSWORD_BYTES));
        }
        String userId = redisTemplate.opsForValue().getAndDelete(TOKEN_PREFIX + hash(request.token().trim()));
        if (userId == null) {
            throw new BusinessException(ErrorCode.PASSWORD_RESET_TOKEN_INVALID);
        }
        redisTemplate.delete(USER_PREFIX + userId);
        User user = userRepository.findById(Long.valueOf(userId))
                .filter(User::isEnabled)
                .orElseThrow(() -> new BusinessException(ErrorCode.PASSWORD_RESET_TOKEN_INVALID));
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setMustChangePassword(false);
        user.setPasswordExpiresAt(null);
        userRepository.saveAndFlush(user);
        refreshTokenService.revokeAll(user.getId());
        loginAttemptLimiter.reset(LoginAttemptLimiter.subjectOfUser(user.getId()));
        log.info("User id={} reset the password; all refresh tokens revoked", user.getId());
    }

    private boolean isCustomerOnly(Long userId) {
        List<String> roles = userRoleRepository.findRoleNamesByUserId(userId);
        return roles.stream().allMatch(RoleName.CUSTOMER.name()::equals);
    }

    private boolean overEmailLimit(String email) {
        String key = EMAIL_PREFIX + hash(email);
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            redisTemplate.expire(key, EMAIL_WINDOW);
        }
        return count != null && count > MAX_REQUESTS_PER_EMAIL;
    }

    private String newToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String hash(String text) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
