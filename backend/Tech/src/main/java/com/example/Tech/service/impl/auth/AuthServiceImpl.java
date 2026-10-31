package com.example.Tech.service.impl.auth;

import com.example.Tech.dto.request.auth.LoginRequest;
import com.example.Tech.dto.request.auth.RefreshTokenRequest;
import com.example.Tech.dto.request.auth.RegisterRequest;
import com.example.Tech.dto.response.auth.AuthResponse;
import com.example.Tech.entity.user.CustomerProfile;
import com.example.Tech.entity.user.Role;
import com.example.Tech.entity.user.RoleName;
import com.example.Tech.entity.user.User;
import com.example.Tech.entity.user.UserRole;
import com.example.Tech.exception.BusinessException;
import com.example.Tech.exception.ErrorCode;
import com.example.Tech.mapper.user.UserMapper;
import com.example.Tech.repository.user.CustomerProfileRepository;
import com.example.Tech.repository.user.RoleRepository;
import com.example.Tech.repository.user.UserRepository;
import com.example.Tech.repository.user.UserRoleRepository;
import com.example.Tech.security.JwtTokenService;
import com.example.Tech.security.RefreshTokenService;
import com.example.Tech.service.auth.AuthService;
import com.example.Tech.service.auth.LoginAttemptLimiter;
import com.example.Tech.service.auth.google.GoogleIdentity;
import com.example.Tech.util.AccountUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@Transactional
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final RefreshTokenService refreshTokenService;
    private final UserMapper userMapper;
    private final Clock clock;
    private final LoginAttemptLimiter loginAttemptLimiter;

    /** Compared against when the login name is unknown, so both failure paths take about the same time. */
    private final String dummyPasswordHash;

    public AuthServiceImpl(UserRepository userRepository, RoleRepository roleRepository,
                           UserRoleRepository userRoleRepository, CustomerProfileRepository customerProfileRepository,
                           PasswordEncoder passwordEncoder, JwtTokenService jwtTokenService,
                           RefreshTokenService refreshTokenService, UserMapper userMapper, Clock clock,
                           LoginAttemptLimiter loginAttemptLimiter) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.customerProfileRepository = customerProfileRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
        this.refreshTokenService = refreshTokenService;
        this.userMapper = userMapper;
        this.clock = clock;
        this.loginAttemptLimiter = loginAttemptLimiter;
        this.dummyPasswordHash = passwordEncoder.encode("timing-protection-dummy-password");
    }

    @Override
    public AuthResponse register(RegisterRequest request) {
        if (AccountUtil.exceedsBcryptLimit(request.password())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "Password must be at most %d bytes".formatted(AccountUtil.MAX_PASSWORD_BYTES));
        }
        User user = userMapper.toEntity(request);
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL,
                    "Email '%s' is already registered".formatted(user.getEmail()));
        }
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new BusinessException(ErrorCode.DUPLICATE_USERNAME,
                    "Username '%s' is already taken".formatted(user.getUsername()));
        }
        user.setPasswordHash(passwordEncoder.encode(request.password()));

        // User, CUSTOMER role and profile are written in this single transaction
        User saved = userRepository.saveAndFlush(user);
        Role customerRole = roleRepository.findByName(RoleName.CUSTOMER.name())
                .orElseThrow(() -> new IllegalStateException("Role CUSTOMER is missing"));
        userRoleRepository.save(new UserRole(saved, customerRole));
        customerProfileRepository.save(new CustomerProfile(saved));
        userRoleRepository.flush();

        log.info("Registered user id={} username={}", saved.getId(), saved.getUsername());
        return issueTokens(saved, List.of(customerRole.getName()));
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        Optional<User> found = findByIdentifier(request.identifier());
        String subject = found.map(user -> LoginAttemptLimiter.subjectOfUser(user.getId()))
                .orElseGet(() -> LoginAttemptLimiter.subjectOfUnknown(AccountUtil.normalize(request.identifier())));
        // A locked account is refused even with the right password
        loginAttemptLimiter.ensureNotLocked(subject);
        if (found.isEmpty() || AccountUtil.exceedsBcryptLimit(request.password())) {
            passwordEncoder.matches("timing-protection", dummyPasswordHash);
            loginAttemptLimiter.recordFailure(subject);
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }
        User user = found.get();
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            loginAttemptLimiter.recordFailure(subject);
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }
        loginAttemptLimiter.reset(subject);
        // Checked only after the password, so the response does not reveal which accounts exist
        ensureEnabled(user);
        ensureTemporaryPasswordValid(user);

        user.setLastLogin(LocalDateTime.now(clock));
        log.info("User id={} logged in", user.getId());
        return issueTokens(user, roleNames(user.getId()));
    }

    @Override
    public AuthResponse refresh(RefreshTokenRequest request) {
        Long userId = refreshTokenService.consume(request.refreshToken());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_TOKEN));
        ensureEnabled(user);
        ensureTemporaryPasswordValid(user);
        return issueTokens(user, roleNames(user.getId()));
    }

    @Override
    public void logout(RefreshTokenRequest request) {
        refreshTokenService.revoke(request.refreshToken());
    }

    @Override
    public AuthResponse loginWithGoogle(GoogleIdentity identity) {
        if (!identity.emailVerified() || identity.email() == null) {
            throw new BusinessException(ErrorCode.GOOGLE_EMAIL_NOT_VERIFIED);
        }
        String email = AccountUtil.normalize(identity.email());
        User user = userRepository.findByGoogleSub(identity.subject()).or(() -> userRepository.findByEmail(email))
                .orElse(null);
        List<String> roles;
        if (user == null) {
            user = createGoogleCustomer(identity, email);
            roles = List.of(RoleName.CUSTOMER.name());
        } else {
            roles = roleNames(user.getId());
            if (!roles.stream().allMatch(RoleName.CUSTOMER.name()::equals)) {
                throw new BusinessException(ErrorCode.GOOGLE_LOGIN_NOT_ALLOWED);
            }
            ensureEnabled(user);
            if (user.getGoogleSub() == null) {
                user.setGoogleSub(identity.subject());
            } else if (!user.getGoogleSub().equals(identity.subject())) {
                // the email belongs to an account linked to another Google account
                throw new BusinessException(ErrorCode.GOOGLE_TOKEN_INVALID);
            }
        }
        user.setLastLogin(LocalDateTime.now(clock));
        log.info("User id={} logged in with Google", user.getId());
        return issueTokens(user, roles);
    }

    private User createGoogleCustomer(GoogleIdentity identity, String email) {
        User user = new User();
        user.setEmail(email);
        user.setUsername(freeUsername(email));
        user.setFullname(truncate(StringUtils.hasText(identity.name()) ? identity.name().trim() : email, 120));
        user.setAvatarUrl(identity.pictureUrl());
        user.setGoogleSub(identity.subject());
        // nobody knows this password: the account signs in with Google, or sets a password through "Quên mật khẩu"
        user.setPasswordHash(passwordEncoder.encode(UUID.randomUUID() + UUID.randomUUID().toString()));
        User saved = userRepository.saveAndFlush(user);
        Role customerRole = roleRepository.findByName(RoleName.CUSTOMER.name())
                .orElseThrow(() -> new IllegalStateException("Role CUSTOMER is missing"));
        userRoleRepository.save(new UserRole(saved, customerRole));
        customerProfileRepository.save(new CustomerProfile(saved));
        userRoleRepository.flush();
        log.info("Registered user id={} through Google", saved.getId());
        return saved;
    }

    /** The part of the email before "@" as a username; a random suffix when it is too short or already taken. */
    private String freeUsername(String email) {
        String base = email.substring(0, email.indexOf('@')).replaceAll("[^a-z0-9._-]", "");
        base = truncate(base.length() < 3 ? "user" + base : base, 40);
        String candidate = base;
        while (userRepository.existsByUsername(candidate)) {
            candidate = base + ThreadLocalRandom.current().nextInt(1000, 10000);
        }
        return candidate;
    }

    private static String truncate(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max);
    }

    private Optional<User> findByIdentifier(String identifier) {
        String normalized = AccountUtil.normalize(identifier);
        return normalized.contains("@")
                ? userRepository.findByEmail(normalized)
                : userRepository.findByUsername(normalized);
    }

    private static void ensureEnabled(User user) {
        if (!user.isEnabled()) {
            throw new BusinessException(ErrorCode.ACCOUNT_DISABLED);
        }
    }

    private void ensureTemporaryPasswordValid(User user) {
        if (user.isTemporaryPasswordExpired(LocalDateTime.now(clock))) {
            throw new BusinessException(ErrorCode.TEMP_PASSWORD_EXPIRED);
        }
    }

    private List<String> roleNames(Long userId) {
        return userRoleRepository.findRoleNamesByUserId(userId);
    }

    private AuthResponse issueTokens(User user, List<String> roles) {
        String accessToken = jwtTokenService.issueAccessToken(user, roles);
        String refreshToken = refreshTokenService.issue(user.getId());
        return new AuthResponse(accessToken, refreshToken, AuthResponse.BEARER,
                jwtTokenService.accessTokenTtlSeconds(), userMapper.toAuthUserResponse(user, roles));
    }
}
