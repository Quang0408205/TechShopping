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
import com.example.Tech.util.AccountUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class EmployeeHiringServiceImpl implements EmployeeHiringService {

    private static final String LOWER = "abcdefghijkmnpqrstuvwxyz";
    private static final String UPPER = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String DIGITS = "23456789";
    private static final String SYMBOLS = "@#$%&*!?";
    private static final int GENERATED_LENGTH = 12;

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
    private final SecureRandom random = new SecureRandom();

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
        String password = request.temporaryPassword() == null || request.temporaryPassword().isBlank()
                ? generatePassword() : request.temporaryPassword();
        if (AccountUtil.exceedsBcryptLimit(password)) {
            throw BusinessException.invalidField("temporaryPassword",
                    "Mật khẩu tạm tối đa %d byte".formatted(AccountUtil.MAX_PASSWORD_BYTES));
        }
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
        return new HireResponse(EmployeeMapper.toResponse(savedEmployee, assignment), role.name(), password);
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

    /** A manager always carries the manager label; STAFF cannot claim it and default to the sales label. */
    private static String positionLabel(RoleName role, String requested) {
        if (role == RoleName.BRANCH_MANAGER) {
            return EmployeeAssignment.BRANCH_MANAGER_POSITION;
        }
        String label = trimToNull(requested);
        if (label == null) {
            return StaffRoles.DEFAULT_POSITION;
        }
        if (StaffRoles.isManagerPosition(label)) {
            throw BusinessException.invalidField("positionAtStore",
                    "Muốn làm Quản lý chi nhánh, hãy tuyển với vai trò Quản lý chi nhánh");
        }
        return label;
    }

    private Store openStore(Integer storeId) {
        Store store = storeRepository.findById(storeId).orElseThrow(() -> new BusinessException(
                ErrorCode.STORE_NOT_FOUND, "Không tìm thấy chi nhánh id %d".formatted(storeId)));
        if (Boolean.FALSE.equals(store.getActive())) {
            throw BusinessException.invalidField("storeId", "Chi nhánh đang tạm đóng");
        }
        return store;
    }

    private String generatePassword() {
        StringBuilder password = new StringBuilder(GENERATED_LENGTH);
        // one of each class first, then fill; shuffled so the first four are not always the same kinds
        for (String pool : new String[]{LOWER, UPPER, DIGITS, SYMBOLS}) {
            password.append(pool.charAt(random.nextInt(pool.length())));
        }
        String all = LOWER + UPPER + DIGITS + SYMBOLS;
        while (password.length() < GENERATED_LENGTH) {
            password.append(all.charAt(random.nextInt(all.length())));
        }
        for (int i = password.length() - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            char swap = password.charAt(i);
            password.setCharAt(i, password.charAt(j));
            password.setCharAt(j, swap);
        }
        return password.toString();
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
