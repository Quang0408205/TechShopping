package com.example.Tech.service.impl.user;

import com.example.Tech.dto.request.user.UserRolesUpdateRequest;
import com.example.Tech.dto.request.user.UserSearchRequest;
import com.example.Tech.dto.request.user.UserStatusUpdateRequest;
import com.example.Tech.dto.response.common.PageResponse;
import com.example.Tech.dto.response.user.UserResponse;
import com.example.Tech.entity.user.Role;
import com.example.Tech.entity.user.RoleName;
import com.example.Tech.entity.user.User;
import com.example.Tech.entity.user.UserRole;
import com.example.Tech.exception.BusinessException;
import com.example.Tech.exception.ErrorCode;
import com.example.Tech.exception.ResourceNotFoundException;
import com.example.Tech.mapper.user.UserMapper;
import com.example.Tech.repository.user.RoleRepository;
import com.example.Tech.repository.user.UserFilterSpecifications;
import com.example.Tech.repository.user.UserRepository;
import com.example.Tech.repository.user.UserRoleName;
import com.example.Tech.repository.user.UserRoleRepository;
import com.example.Tech.security.RefreshTokenService;
import com.example.Tech.service.user.AdminUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminUserServiceImpl implements AdminUserService {

    private static final String ADMIN = RoleName.ADMIN.name();

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final RefreshTokenService refreshTokenService;
    private final UserMapper userMapper;
    private final Clock clock;

    @Override
    public PageResponse<UserResponse> search(Long adminId, UserSearchRequest filter, Pageable pageable) {
        ensureActingAdmin(adminId);
        Page<User> page = userRepository.findAll(UserFilterSpecifications.matching(filter), pageable);

        // Roles of the whole page in one query (no N+1)
        List<Long> ids = page.getContent().stream().map(User::getId).toList();
        Map<Long, List<String>> rolesByUser = ids.isEmpty() ? Map.of()
                : userRoleRepository.findRoleNamesByUserIds(ids).stream()
                .collect(Collectors.groupingBy(UserRoleName::getUserId, LinkedHashMap::new,
                        Collectors.mapping(UserRoleName::getRoleName, Collectors.toList())));

        return PageResponse.from(page.map(user ->
                userMapper.toResponse(user, rolesByUser.getOrDefault(user.getId(), List.of()))));
    }

    @Override
    public UserResponse getById(Long adminId, Long userId) {
        ensureActingAdmin(adminId);
        User user = findUser(userId);
        return userMapper.toResponse(user, userRoleRepository.findRoleNamesByUserId(userId));
    }

    @Override
    @Transactional
    public UserResponse updateStatus(Long adminId, Long userId, UserStatusUpdateRequest request) {
        ensureActingAdmin(adminId);
        ensureNotSelf(adminId, userId);
        User user = findUser(userId);
        ensureNotDeleted(user);
        List<String> roles = userRoleRepository.findRoleNamesByUserId(userId);

        boolean activate = request.active();
        if (!activate) {
            ensureNotLastAdmin(user, roles);
        }
        user.setActive(activate);
        User saved = userRepository.saveAndFlush(user);
        if (!activate) {
            refreshTokenService.revokeAll(userId);
        }
        log.info("Admin id={} set user id={} active={}", adminId, userId, activate);
        return userMapper.toResponse(saved, roles);
    }

    @Override
    @Transactional
    public UserResponse updateRoles(Long adminId, Long userId, UserRolesUpdateRequest request) {
        ensureActingAdmin(adminId);
        User user = findUser(userId);
        ensureNotDeleted(user);

        Set<String> requested = request.roles().stream()
                .map(name -> name.trim().toUpperCase(Locale.ROOT))
                .collect(Collectors.toCollection(TreeSet::new));
        Map<String, Role> newRoles = new LinkedHashMap<>();
        for (String name : requested) {
            Role role = roleRepository.findByName(name)
                    .orElseThrow(() -> new BusinessException(ErrorCode.ROLE_NOT_FOUND,
                            "Role '%s' does not exist".formatted(name)));
            newRoles.put(name, role);
        }

        List<UserRole> current = userRoleRepository.findAllByIdUserId(userId);
        List<String> currentNames = current.stream().map(userRole -> userRole.getRole().getName()).toList();
        boolean removesAdmin = currentNames.contains(ADMIN) && !newRoles.containsKey(ADMIN);
        if (removesAdmin) {
            if (userId.equals(adminId)) {
                throw new BusinessException(ErrorCode.CANNOT_MODIFY_OWN_ACCOUNT);
            }
            ensureNotLastAdmin(user, currentNames);
        }

        List<UserRole> toRemove = new ArrayList<>();
        for (UserRole userRole : current) {
            if (!newRoles.containsKey(userRole.getRole().getName())) {
                toRemove.add(userRole);
            }
        }
        userRoleRepository.deleteAll(toRemove);
        for (Role role : newRoles.values()) {
            if (!currentNames.contains(role.getName())) {
                userRoleRepository.save(new UserRole(user, role));
            }
        }
        userRoleRepository.flush();

        // No token revocation: /auth/refresh re-reads the roles, so they apply within one access-token lifetime
        log.info("Admin id={} set roles of user id={} to {}", adminId, userId, newRoles.keySet());
        return userMapper.toResponse(user, List.copyOf(newRoles.keySet()));
    }

    @Override
    @Transactional
    public void delete(Long adminId, Long userId) {
        ensureActingAdmin(adminId);
        ensureNotSelf(adminId, userId);
        User user = findUser(userId);
        if (user.getDeletedAt() != null) {
            return;
        }
        ensureNotLastAdmin(user, userRoleRepository.findRoleNamesByUserId(userId));

        user.setDeletedAt(LocalDateTime.now(clock));
        user.setActive(false);
        userRepository.saveAndFlush(user);
        refreshTokenService.revokeAll(userId);
        log.info("Admin id={} soft-deleted user id={}", adminId, userId);
    }

    /**
     * The access token may be up to 30 min old, so the caller's account and ADMIN role are re-checked.
     */
    private void ensureActingAdmin(Long adminId) {
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_TOKEN));
        if (!admin.isEnabled()) {
            throw new BusinessException(ErrorCode.ACCOUNT_DISABLED);
        }
        if (!userRoleRepository.findRoleNamesByUserId(adminId).contains(ADMIN)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
    }

    private static void ensureNotSelf(Long adminId, Long userId) {
        if (userId.equals(adminId)) {
            throw new BusinessException(ErrorCode.CANNOT_MODIFY_OWN_ACCOUNT);
        }
    }

    private static void ensureNotDeleted(User user) {
        if (user.getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.USER_DELETED);
        }
    }

    /** Refuses to disable, delete or demote the only enabled ADMIN. */
    private void ensureNotLastAdmin(User user, List<String> roles) {
        if (roles.contains(ADMIN) && user.isEnabled() && userRoleRepository.countEnabledUsersWithRole(ADMIN) <= 1) {
            throw new BusinessException(ErrorCode.LAST_ADMIN);
        }
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND, userId));
    }
}
