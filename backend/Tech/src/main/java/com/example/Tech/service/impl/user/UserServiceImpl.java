package com.example.Tech.service.impl.user;

import com.example.Tech.dto.request.user.ChangePasswordRequest;
import com.example.Tech.dto.request.user.CustomerProfileUpdateRequest;
import com.example.Tech.dto.request.user.UserUpdateRequest;
import com.example.Tech.dto.response.user.CustomerProfileResponse;
import com.example.Tech.dto.response.user.UserResponse;
import com.example.Tech.entity.user.CustomerProfile;
import com.example.Tech.entity.user.User;
import com.example.Tech.exception.BusinessException;
import com.example.Tech.exception.ErrorCode;
import com.example.Tech.mapper.user.CustomerProfileMapper;
import com.example.Tech.mapper.user.UserMapper;
import com.example.Tech.repository.user.CustomerProfileRepository;
import com.example.Tech.repository.user.UserRepository;
import com.example.Tech.repository.user.UserRoleRepository;
import com.example.Tech.security.RefreshTokenService;
import com.example.Tech.service.user.UserService;
import com.example.Tech.util.AccountUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final UserMapper userMapper;
    private final CustomerProfileMapper customerProfileMapper;

    @Override
    public UserResponse getMe(Long userId) {
        User user = loadCurrentUser(userId);
        return userMapper.toResponse(user, userRoleRepository.findRoleNamesByUserId(userId));
    }

    @Override
    @Transactional
    public UserResponse updateMe(Long userId, UserUpdateRequest request) {
        User user = loadCurrentUser(userId);
        userMapper.updateEntity(user, request);
        User saved = userRepository.saveAndFlush(user);
        log.info("User id={} updated own account", userId);
        return userMapper.toResponse(saved, userRoleRepository.findRoleNamesByUserId(userId));
    }

    @Override
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = loadCurrentUser(userId);
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.INVALID_PASSWORD);
        }
        if (AccountUtil.exceedsBcryptLimit(request.newPassword())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "New password must be at most %d bytes".formatted(AccountUtil.MAX_PASSWORD_BYTES));
        }
        if (request.newPassword().equals(request.currentPassword())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "New password must be different from the current password");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.saveAndFlush(user);
        refreshTokenService.revokeAll(userId);
        log.info("User id={} changed password; all refresh tokens revoked", userId);
    }

    @Override
    public CustomerProfileResponse getMyProfile(Long userId) {
        loadCurrentUser(userId);
        return customerProfileRepository.findById(userId)
                .map(customerProfileMapper::toResponse)
                .orElseThrow(() -> new BusinessException(ErrorCode.CUSTOMER_PROFILE_NOT_FOUND));
    }

    @Override
    @Transactional
    public CustomerProfileResponse updateMyProfile(Long userId, CustomerProfileUpdateRequest request) {
        User user = loadCurrentUser(userId);
        CustomerProfile profile = customerProfileRepository.findById(userId)
                .orElseGet(() -> new CustomerProfile(user));
        customerProfileMapper.updateEntity(profile, request);
        CustomerProfile saved = customerProfileRepository.saveAndFlush(profile);
        log.info("User id={} updated own customer profile", userId);
        return customerProfileMapper.toResponse(saved);
    }

    /**
     * The access token may outlive a deleted or deactivated account (up to 30 min),
     * so the account is re-checked on every call.
     */
    private User loadCurrentUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_TOKEN));
        if (!user.isEnabled()) {
            throw new BusinessException(ErrorCode.ACCOUNT_DISABLED);
        }
        return user;
    }
}
