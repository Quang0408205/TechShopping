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
import com.example.Tech.util.AccountUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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

    /** Compared against when the login name is unknown, so both failure paths take about the same time. */
    private final String dummyPasswordHash;

    public AuthServiceImpl(UserRepository userRepository, RoleRepository roleRepository,
                           UserRoleRepository userRoleRepository, CustomerProfileRepository customerProfileRepository,
                           PasswordEncoder passwordEncoder, JwtTokenService jwtTokenService,
                           RefreshTokenService refreshTokenService, UserMapper userMapper, Clock clock) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.customerProfileRepository = customerProfileRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
        this.refreshTokenService = refreshTokenService;
        this.userMapper = userMapper;
        this.clock = clock;
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
        if (found.isEmpty() || AccountUtil.exceedsBcryptLimit(request.password())) {
            passwordEncoder.matches("timing-protection", dummyPasswordHash);
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }
        User user = found.get();
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }
        // Checked only after the password, so the response does not reveal which accounts exist
        ensureEnabled(user);

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
        return issueTokens(user, roleNames(user.getId()));
    }

    @Override
    public void logout(RefreshTokenRequest request) {
        refreshTokenService.revoke(request.refreshToken());
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
