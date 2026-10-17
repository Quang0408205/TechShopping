package com.example.Tech.service.impl.user;

import com.example.Tech.dto.request.auth.RegisterRequest;
import com.example.Tech.dto.request.user.AdminCreateRequest;
import com.example.Tech.dto.request.user.UserRolesUpdateRequest;
import com.example.Tech.dto.request.user.UserSearchRequest;
import com.example.Tech.dto.request.user.UserStatusUpdateRequest;
import com.example.Tech.dto.response.common.PageResponse;
import com.example.Tech.dto.response.user.AdminCreatedResponse;
import com.example.Tech.dto.response.user.UserResponse;
import com.example.Tech.entity.employee.EmployeeAssignment;
import com.example.Tech.entity.user.Role;
import com.example.Tech.entity.user.RoleName;
import com.example.Tech.entity.user.UserRole;
import com.example.Tech.entity.user.User;
import com.example.Tech.exception.BusinessException;
import com.example.Tech.exception.ErrorCode;
import com.example.Tech.exception.ResourceNotFoundException;
import com.example.Tech.mapper.user.UserMapper;
import com.example.Tech.repository.employee.EmployeeAssignmentRepository;
import com.example.Tech.repository.user.RoleRepository;
import com.example.Tech.repository.user.UserFilterSpecifications;
import com.example.Tech.repository.user.UserRepository;
import com.example.Tech.repository.user.UserRoleName;
import com.example.Tech.repository.user.UserRoleRepository;
import com.example.Tech.security.RefreshTokenService;
import com.example.Tech.service.employee.StaffRoles;
import com.example.Tech.service.user.AdminUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminUserServiceImpl implements AdminUserService {

    private static final String ADMIN = RoleName.ADMIN.name();

    private static final String STAFF = RoleName.STAFF.name();

    private static final String MANAGER = RoleName.BRANCH_MANAGER.name();

    /** Days a new administrator has to replace the temporary password (same as hired staff). */
    static final int TEMPORARY_PASSWORD_DAYS = 7;
    static final int TEMPORARY_PASSWORD_LENGTH = 12;
    /** No look-alike characters (0 O 1 l I), so the password can be read out or typed from a message. */
    private static final String PASSWORD_UPPER = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String PASSWORD_LOWER = "abcdefghijkmnopqrstuvwxyz";
    private static final String PASSWORD_DIGITS = "23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final EmployeeAssignmentRepository assignmentRepository;
    private final StaffRoles staffRoles;
    private final UserRoleRepository userRoleRepository;
    private final RefreshTokenService refreshTokenService;
    private final UserMapper userMapper;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    @Override
    public PageResponse<UserResponse> search(Long adminId, UserSearchRequest filter, Pageable pageable) {
        ensureActingAdmin(adminId);
        if (filter.createdFrom() != null && filter.createdTo() != null && filter.createdFrom().isAfter(filter.createdTo())) {
            throw BusinessException.invalidField("createdTo", "Ngày kết thúc phải sau hoặc bằng ngày bắt đầu");
        }
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
    public AdminCreatedResponse createAdmin(Long adminId, AdminCreateRequest request) {
        ensureActingAdmin(adminId);
        String password = randomPassword();
        User user = userMapper.toEntity(new RegisterRequest(request.email(), request.username(), password,
                request.fullname(), request.phone()));
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL, "Email %s đã được đăng ký".formatted(user.getEmail()));
        }
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new BusinessException(ErrorCode.DUPLICATE_USERNAME,
                    "Tên đăng nhập %s đã được dùng".formatted(user.getUsername()));
        }
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setMustChangePassword(true);
        LocalDateTime expiresAt = LocalDateTime.now(clock).plusDays(TEMPORARY_PASSWORD_DAYS);
        user.setPasswordExpiresAt(expiresAt);
        User saved = userRepository.saveAndFlush(user);
        Role role = roleRepository.findByName(ADMIN)
                .orElseThrow(() -> new IllegalStateException("Role " + ADMIN + " is missing"));
        userRoleRepository.save(new UserRole(saved, role));
        // the password is only in the response; it is never logged
        log.info("Admin id={} created administrator id={} (username={})", adminId, saved.getId(), saved.getUsername());
        return new AdminCreatedResponse(userMapper.toResponse(saved, List.of(ADMIN)), password, expiresAt);
    }

    /** 12 random characters with at least one upper-case letter, one lower-case letter and one digit. */
    static String randomPassword() {
        String all = PASSWORD_UPPER + PASSWORD_LOWER + PASSWORD_DIGITS;
        char[] chars = new char[TEMPORARY_PASSWORD_LENGTH];
        chars[0] = PASSWORD_UPPER.charAt(RANDOM.nextInt(PASSWORD_UPPER.length()));
        chars[1] = PASSWORD_LOWER.charAt(RANDOM.nextInt(PASSWORD_LOWER.length()));
        chars[2] = PASSWORD_DIGITS.charAt(RANDOM.nextInt(PASSWORD_DIGITS.length()));
        for (int i = 3; i < chars.length; i++) {
            chars[i] = all.charAt(RANDOM.nextInt(all.length()));
        }
        for (int i = chars.length - 1; i > 0; i--) {
            int j = RANDOM.nextInt(i + 1);
            char swap = chars[i];
            chars[i] = chars[j];
            chars[j] = swap;
        }
        return new String(chars);
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
            // a locked employee leaves their store; a manager goes back to STAFF
            if (staffRoles.endAssignment(saved)) {
                roles = userRoleRepository.findRoleNamesByUserId(userId);
            }
        }
        log.info("Admin id={} set user id={} active={}", adminId, userId, activate);
        return userMapper.toResponse(saved, roles);
    }

    @Override
    @Transactional
    public UserResponse updateRoles(Long adminId, Long userId, UserRolesUpdateRequest request) {
        ensureActingAdmin(adminId);
        RoleName target = assignableRole(request.roles());
        User user = findUser(userId);
        ensureNotDeleted(user);

        List<String> currentNames = userRoleRepository.findRoleNamesByUserId(userId);
        if (currentNames.contains(ADMIN)) {
            throw userId.equals(adminId) ? new BusinessException(ErrorCode.CANNOT_MODIFY_OWN_ACCOUNT)
                    : new BusinessException(ErrorCode.ACCESS_DENIED, "Không đổi vai trò của tài khoản quản trị viên");
        }
        if (!currentNames.contains(STAFF) && !currentNames.contains(MANAGER)) {
            throw new BusinessException(ErrorCode.CUSTOMER_ACCOUNT_NOT_ELIGIBLE);
        }
        if (currentNames.equals(List.of(target.name()))) {
            return userMapper.toResponse(user, currentNames);
        }

        EmployeeAssignment assignment = assignmentRepository.findActiveWithStoreByUserId(userId).orElse(null);
        if (target == RoleName.BRANCH_MANAGER) {
            if (assignment == null) {
                throw new BusinessException(ErrorCode.MANAGER_REQUIRES_ASSIGNMENT);
            }
            staffRoles.requireFreeManagerSeat(assignment.getStore().getId(), userId);
            assignment.setPositionAtStore(EmployeeAssignment.BRANCH_MANAGER_POSITION);
        } else if (assignment != null && StaffRoles.isManagerPosition(assignment.getPositionAtStore())) {
            assignment.setPositionAtStore(StaffRoles.DEFAULT_POSITION);
        }
        staffRoles.setRole(user, target);
        refreshTokenService.revokeAll(userId);
        log.info("Admin id={} set the role of user id={} to {}", adminId, userId, target);
        return userMapper.toResponse(user, List.of(target.name()));
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
        staffRoles.endAssignment(user);
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

    /** The request must name exactly one of STAFF / BRANCH_MANAGER: customers and admins are never assigned here. */
    private static RoleName assignableRole(List<String> requested) {
        if (requested.size() == 1) {
            String name = requested.get(0).trim().toUpperCase(Locale.ROOT);
            if (name.equals(STAFF) || name.equals(MANAGER)) {
                return RoleName.valueOf(name);
            }
        }
        throw new BusinessException(ErrorCode.ROLE_NOT_ASSIGNABLE);
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
