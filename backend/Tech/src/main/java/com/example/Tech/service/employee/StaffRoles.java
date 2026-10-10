package com.example.Tech.service.employee;

import com.example.Tech.entity.employee.EmployeeAssignment;
import com.example.Tech.entity.user.Role;
import com.example.Tech.entity.user.RoleName;
import com.example.Tech.entity.user.User;
import com.example.Tech.entity.user.UserRole;
import com.example.Tech.exception.BusinessException;
import com.example.Tech.exception.ErrorCode;
import com.example.Tech.repository.employee.EmployeeAssignmentRepository;
import com.example.Tech.repository.store.StoreRepository;
import com.example.Tech.repository.user.RoleRepository;
import com.example.Tech.repository.user.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

/**
 * Rules shared by the user and employee services for internal accounts: an internal account holds exactly one of
 * STAFF / BRANCH_MANAGER (ADMIN accounts are never touched here), a store has at most one enabled BRANCH_MANAGER,
 * and a manager always has a current assignment. The role is the source of truth; position_at_store is only a label.
 * Methods join the caller's transaction; the caller revokes the refresh tokens.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StaffRoles {

    /** Label given to a manager who goes back to STAFF. */
    public static final String DEFAULT_POSITION = "Nhân viên bán hàng";

    private static final String MANAGER = RoleName.BRANCH_MANAGER.name();

    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final EmployeeAssignmentRepository assignmentRepository;
    private final StoreRepository storeRepository;
    private final Clock clock;

    public static boolean isManagerPosition(String position) {
        return EmployeeAssignment.BRANCH_MANAGER_POSITION.equalsIgnoreCase(position == null ? "" : position.trim());
    }

    public boolean isManager(Long userId) {
        return userRoleRepository.findRoleNamesByUserId(userId).contains(MANAGER);
    }

    /**
     * Locks the store row, then 409 STORE_ALREADY_HAS_MANAGER when an enabled manager other than {@code userId}
     * already works there. The lock makes two concurrent promotions into the same store queue up.
     */
    public void requireFreeManagerSeat(Integer storeId, Long userId) {
        storeRepository.findByIdForUpdate(storeId);
        boolean taken = assignmentRepository.findActiveManagerUserIds(storeId).stream()
                .anyMatch(managerId -> !managerId.equals(userId));
        if (taken) {
            throw new BusinessException(ErrorCode.STORE_ALREADY_HAS_MANAGER);
        }
    }

    /** The account ends up with exactly this one internal role (CUSTOMER and the other staff role are dropped). */
    public void setRole(User user, RoleName target) {
        List<UserRole> current = userRoleRepository.findAllByIdUserId(user.getId());
        if (current.stream().anyMatch(userRole -> RoleName.ADMIN.name().equals(userRole.getRole().getName()))) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "Không đổi vai trò của tài khoản quản trị viên");
        }
        boolean present = false;
        for (UserRole userRole : current) {
            if (userRole.getRole().getName().equals(target.name())) {
                present = true;
            } else {
                userRoleRepository.delete(userRole);
            }
        }
        if (!present) {
            Role role = roleRepository.findByName(target.name()).orElseThrow(() -> new BusinessException(
                    ErrorCode.ROLE_NOT_FOUND, "Role '%s' does not exist".formatted(target.name())));
            userRoleRepository.save(new UserRole(user, role));
        }
        userRoleRepository.flush();
    }

    /**
     * Closes the current assignment (if any); a manager without an assignment is no longer allowed, so they go
     * back to STAFF. Returns true when something changed (the caller then revokes the sessions).
     */
    public boolean endAssignment(User user) {
        EmployeeAssignment current = assignmentRepository.findActiveWithStoreByUserId(user.getId()).orElse(null);
        boolean manager = isManager(user.getId());
        if (current != null) {
            current.setActive(false);
            current.setEndDate(LocalDate.now(clock));
            assignmentRepository.saveAndFlush(current);
        }
        if (manager) {
            setRole(user, RoleName.STAFF);
            log.info("Manager id={} lost their assignment and is STAFF again", user.getId());
        }
        return current != null || manager;
    }
}
