package com.example.Tech.service.impl.employee;

import com.example.Tech.dto.request.auth.RegisterRequest;
import com.example.Tech.dto.request.employee.HireRequest;
import com.example.Tech.dto.response.employee.HireResponse;
import com.example.Tech.entity.employee.Employee;
import com.example.Tech.entity.employee.EmployeeAssignment;
import com.example.Tech.entity.store.Store;
import com.example.Tech.entity.user.Role;
import com.example.Tech.entity.user.RoleName;
import com.example.Tech.entity.user.User;
import com.example.Tech.entity.user.UserRole;
import com.example.Tech.exception.BusinessException;
import com.example.Tech.exception.ErrorCode;
import com.example.Tech.mapper.employee.EmployeeMapper;
import com.example.Tech.mapper.user.UserMapper;
import com.example.Tech.repository.employee.EmployeeAssignmentRepository;
import com.example.Tech.repository.employee.EmployeeRepository;
import com.example.Tech.repository.store.StoreRepository;
import com.example.Tech.repository.user.RoleRepository;
import com.example.Tech.repository.user.UserRepository;
import com.example.Tech.repository.user.UserRoleRepository;
import com.example.Tech.service.employee.EmployeeHiringService;
import com.example.Tech.service.employee.StaffRoles;
import com.example.Tech.service.user.CurrentUserLoader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class EmployeeHiringServiceImpl implements EmployeeHiringService {

    /** Every new account starts with this password; it must be replaced at the first login (and expires, see below). */
    static final String DEFAULT_TEMPORARY_PASSWORD = "1";
    /** Days a new employee has to replace the temporary password. */
    static final int TEMPORARY_PASSWORD_DAYS = 7;

    private final CurrentUserLoader currentUserLoader;
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;
    private final EmployeeRepository employeeRepository;
    private final EmployeeAssignmentRepository assignmentRepository;
    private final StoreRepository storeRepository;
    private final StaffRoles staffRoles;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    @Override
    public HireResponse hireAsAdmin(Long adminId, HireRequest request) {
        currentUserLoader.loadWithAnyRole(adminId, RoleName.ADMIN);
        RoleName role = assignableRole(request.role());
        if (request.storeId() == null) {
            throw BusinessException.invalidField("storeId", "Vui lòng chọn chi nhánh");
        }
        return hire(adminId, request, role, openStore(request.storeId()));
    }

    @Override
    public HireResponse hireAsManager(Long managerId, HireRequest request) {
        currentUserLoader.loadWithAnyRole(managerId, RoleName.BRANCH_MANAGER);
        if (request.role() != null && !request.role().isBlank() && assignableRole(request.role()) != RoleName.STAFF) {
            throw new BusinessException(ErrorCode.ROLE_NOT_ASSIGNABLE, "Quản lý chi nhánh chỉ được tuyển nhân viên");
        }
        EmployeeAssignment own = assignmentRepository.findActiveWithStoreByUserId(managerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NO_ACTIVE_STORE_ASSIGNMENT));
        return hire(managerId, request, RoleName.STAFF, openStore(own.getStore().getId()));
    }

    private HireResponse hire(Long actorId, HireRequest request, RoleName role, Store store) {
        String password = DEFAULT_TEMPORARY_PASSWORD;
        User user = userMapper.toEntity(new RegisterRequest(request.email(), request.username(), password,
                request.fullname(), request.phone()));
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL,
                    "Email %s đã được đăng ký".formatted(user.getEmail()));
        }
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new BusinessException(ErrorCode.DUPLICATE_USERNAME,
                    "Tên đăng nhập %s đã được dùng".formatted(user.getUsername()));
        }
        String code = trimToNull(request.employeeCode());
        if (code != null && employeeRepository.existsByEmployeeCode(code)) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMPLOYEE_CODE, "Mã nhân viên %s đã được dùng".formatted(code));
        }
        String label = positionLabel(role, request.positionAtStore());
        if (role == RoleName.BRANCH_MANAGER) {
            staffRoles.requireFreeManagerSeat(store.getId(), null);
        }

        user.setPasswordHash(passwordEncoder.encode(password));
        user.setMustChangePassword(true);
        LocalDateTime passwordExpiresAt = LocalDateTime.now(clock).plusDays(TEMPORARY_PASSWORD_DAYS);
        user.setPasswordExpiresAt(passwordExpiresAt);
        User savedUser = userRepository.saveAndFlush(user);
        Role roleEntity = roleRepository.findByName(role.name())
                .orElseThrow(() -> new IllegalStateException("Role " + role + " is missing"));
        userRoleRepository.save(new UserRole(savedUser, roleEntity));

        Employee employee = new Employee();
        employee.setUser(savedUser);
        employee.setEmployeeCode(code);
        employee.setDepartment(trimToNull(request.department()));
        employee.setPosition(trimToNull(request.position()));
        employee.setSalary(request.salary());
        employee.setHiringDate(request.hiringDate() == null ? LocalDate.now(clock) : request.hiringDate());
        employee.setActive(true);
        Employee savedEmployee = employeeRepository.save(employee);

        EmployeeAssignment assignment = new EmployeeAssignment();
        assignment.setEmployee(savedEmployee);
        assignment.setStore(store);
        assignment.setPositionAtStore(label);
        assignment.setStartDate(LocalDate.now(clock));
        assignment.setActive(true);
        assignmentRepository.saveAndFlush(assignment);

        // the password is only in the response; it is never logged
        log.info("User id={} hired {} id={} (employee id={}) into store id={}", actorId, role, savedUser.getId(),
                savedEmployee.getId(), store.getId());
        return new HireResponse(EmployeeMapper.toResponse(savedEmployee, assignment), role.name(), password, passwordExpiresAt);
    }

    /** STAFF or BRANCH_MANAGER only: customers and administrators are never created here. */
    private static RoleName assignableRole(String name) {
        String normalized = name == null ? "" : name.trim().toUpperCase(Locale.ROOT);
        if (normalized.equals(RoleName.STAFF.name())) {
            return RoleName.STAFF;
        }
        if (normalized.equals(RoleName.BRANCH_MANAGER.name())) {
            return RoleName.BRANCH_MANAGER;
        }
        throw new BusinessException(ErrorCode.ROLE_NOT_ASSIGNABLE);
    }

    /** A manager always carries the manager label; STAFF cannot claim it and always get the staff label. */
    private static String positionLabel(RoleName role, String requested) {
        if (role == RoleName.BRANCH_MANAGER) {
            return EmployeeAssignment.BRANCH_MANAGER_POSITION;
        }
        if (StaffRoles.isManagerPosition(requested)) {
            throw BusinessException.invalidField("positionAtStore",
                    "Muốn làm Quản lý chi nhánh, hãy tuyển với vai trò Quản lý chi nhánh");
        }
        return StaffRoles.DEFAULT_POSITION;
    }

    private Store openStore(Integer storeId) {
        Store store = storeRepository.findById(storeId).orElseThrow(() -> new BusinessException(
                ErrorCode.STORE_NOT_FOUND, "Không tìm thấy chi nhánh id %d".formatted(storeId)));
        if (Boolean.FALSE.equals(store.getActive())) {
            throw BusinessException.invalidField("storeId", "Chi nhánh đang tạm đóng");
        }
        return store;
    }


    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
