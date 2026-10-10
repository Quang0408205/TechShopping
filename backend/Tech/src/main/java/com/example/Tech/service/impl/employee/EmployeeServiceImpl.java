package com.example.Tech.service.impl.employee;

import com.example.Tech.dto.request.employee.AssignmentRequest;
import com.example.Tech.dto.request.employee.EmployeeCreateRequest;
import com.example.Tech.dto.request.employee.EmployeeSearchRequest;
import com.example.Tech.dto.request.employee.EmployeeUpdateRequest;
import com.example.Tech.dto.response.common.PageResponse;
import com.example.Tech.dto.response.employee.EmployeeResponse;
import com.example.Tech.entity.employee.Employee;
import com.example.Tech.entity.employee.EmployeeAssignment;
import com.example.Tech.entity.store.Store;
import com.example.Tech.entity.user.RoleName;
import com.example.Tech.entity.user.User;
import com.example.Tech.exception.BusinessException;
import com.example.Tech.exception.ErrorCode;
import com.example.Tech.mapper.employee.EmployeeMapper;
import com.example.Tech.repository.employee.EmployeeAssignmentRepository;
import com.example.Tech.repository.employee.EmployeeFilterSpecifications;
import com.example.Tech.repository.employee.EmployeeRepository;
import com.example.Tech.repository.store.StoreRepository;
import com.example.Tech.repository.user.UserRepository;
import com.example.Tech.repository.user.UserRoleRepository;
import com.example.Tech.security.RefreshTokenService;
import com.example.Tech.service.employee.EmployeeService;
import com.example.Tech.service.employee.StaffRoles;
import com.example.Tech.service.user.CurrentUserLoader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final EmployeeAssignmentRepository assignmentRepository;
    private final StoreRepository storeRepository;
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final CurrentUserLoader currentUserLoader;
    private final StaffRoles staffRoles;
    private final RefreshTokenService refreshTokenService;
    private final Clock clock;

    @Override
    public PageResponse<EmployeeResponse> search(Long adminId, EmployeeSearchRequest filter, Pageable pageable) {
        ensureAdmin(adminId);
        Page<Employee> page = employeeRepository.findAll(EmployeeFilterSpecifications.matching(filter), pageable);
        Map<Long, EmployeeAssignment> current = currentAssignments(page.getContent().stream().map(Employee::getId).toList());
        return PageResponse.from(page.map(employee -> toResponse(employee, current.get(employee.getId()))));
    }

    @Override
    public EmployeeResponse getById(Long adminId, Long employeeId) {
        ensureAdmin(adminId);
        return toResponse(employeeRepository.findWithUserById(employeeId).orElseThrow(() -> notFound(employeeId)));
    }

    @Override
    @Transactional
    public EmployeeResponse create(Long adminId, EmployeeCreateRequest request) {
        ensureAdmin(adminId);
        User user = userRepository.findById(request.userId())
                .filter(found -> found.getDeletedAt() == null)
                .orElseThrow(() -> BusinessException.invalidField("userId", "Không tìm thấy tài khoản"));
        if (!user.isEnabled()) {
            throw BusinessException.invalidField("userId", "Tài khoản đang bị khoá");
        }
        List<String> roles = userRoleRepository.findRoleNamesByUserId(user.getId());
        if (roles.contains(RoleName.CUSTOMER.name())) {
            throw new BusinessException(ErrorCode.CUSTOMER_ACCOUNT_NOT_ELIGIBLE);
        }
        if (!roles.contains(RoleName.STAFF.name())) {
            throw BusinessException.invalidField("userId",
                    "Tài khoản chưa có quyền Nhân viên: cấp quyền ở trang Người dùng & phân quyền trước");
        }
        if (employeeRepository.existsByUserId(user.getId())) {
            throw new BusinessException(ErrorCode.EMPLOYEE_ALREADY_EXISTS);
        }
        String code = trimToNull(request.employeeCode());
        if (code != null && employeeRepository.existsByEmployeeCode(code)) {
            throw duplicateCode(code);
        }
        if (request.storeId() == null && trimToNull(request.positionAtStore()) != null) {
            throw BusinessException.invalidField("storeId", "Chọn chi nhánh khi nhập vị trí tại chi nhánh");
        }
        rejectManagerLabel(user.getId(), request.positionAtStore());
        Store store = request.storeId() == null ? null : openStore(request.storeId());

        Employee employee = new Employee();
        employee.setUser(user);
        employee.setEmployeeCode(code);
        employee.setDepartment(trimToNull(request.department()));
        employee.setPosition(trimToNull(request.position()));
        employee.setSalary(request.salary());
        employee.setHiringDate(request.hiringDate());
        employee.setActive(true);
        Employee saved = employeeRepository.save(employee);
        EmployeeAssignment assignment = store == null ? null
                : startAssignment(saved, store, request.positionAtStore(), today());
        log.info("Admin id={} created employee id={} for user id={} (store {})", adminId, saved.getId(), user.getId(),
                store == null ? "-" : store.getId());
        return toResponse(saved, assignment);
    }

    @Override
    @Transactional
    public EmployeeResponse update(Long adminId, Long employeeId, EmployeeUpdateRequest request) {
        ensureAdmin(adminId);
        Employee employee = lock(employeeId);
        String code = trimToNull(request.employeeCode());
        if (code != null && employeeRepository.existsByEmployeeCodeAndIdNot(code, employeeId)) {
            throw duplicateCode(code);
        }
        employee.setEmployeeCode(code);
        employee.setDepartment(trimToNull(request.department()));
        employee.setPosition(trimToNull(request.position()));
        employee.setSalary(request.salary());
        employee.setHiringDate(request.hiringDate());
        boolean active = !Boolean.FALSE.equals(request.active());
        employee.setActive(active);
        EmployeeAssignment current = findCurrent(employeeId).orElse(null);
        boolean left = false;
        if (!active) {
            if (current != null) {
                close(current);
                current = null;
                left = true;
            }
            left |= staffRoles.endAssignment(employee.getUser());
        }
        employeeRepository.saveAndFlush(employee);
        if (left) {
            refreshTokenService.revokeAll(employee.getUser().getId());
        }
        log.info("Admin id={} updated employee id={} (active={})", adminId, employeeId, active);
        return toResponse(employee, current);
    }

    @Override
    @Transactional
    public EmployeeResponse assign(Long adminId, Long employeeId, AssignmentRequest request) {
        ensureAdmin(adminId);
        Employee employee = lock(employeeId);
        if (Boolean.FALSE.equals(employee.getActive())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Nhân viên đã nghỉ, không gán chi nhánh được");
        }
        Store store = openStore(request.storeId());
        Long userId = employee.getUser().getId();
        boolean manager = staffRoles.isManager(userId);
        // the role decides: a manager is always labelled as one, and nobody becomes a manager through the label
        String position = manager ? EmployeeAssignment.BRANCH_MANAGER_POSITION : request.positionAtStore();
        rejectManagerLabel(userId, position);
        if (manager) {
            staffRoles.requireFreeManagerSeat(store.getId(), userId);
        }
        EmployeeAssignment current = findCurrent(employeeId).orElse(null);
        EmployeeAssignment result;
        if (current != null && current.getStore().getId().equals(store.getId())) {
            current.setPositionAtStore(trimToNull(position));
            result = assignmentRepository.save(current);
        } else {
            if (current != null) {
                close(current);
            }
            LocalDate start = request.startDate() == null ? today() : request.startDate();
            result = startAssignment(employee, store, position, start);
            if (current != null) {
                refreshTokenService.revokeAll(userId);
            }
        }
        log.info("Admin id={} assigned employee id={} to store id={}", adminId, employeeId, store.getId());
        return toResponse(employee, result);
    }

    @Override
    @Transactional
    public EmployeeResponse unassign(Long adminId, Long employeeId) {
        ensureAdmin(adminId);
        Employee employee = lock(employeeId);
        EmployeeAssignment current = findCurrent(employeeId).orElse(null);
        if (current != null) {
            close(current);
            log.info("Admin id={} ended the assignment of employee id={} at store id={}",
                    adminId, employeeId, current.getStore().getId());
        }
        // a manager must have a store: without one they are STAFF again
        if (staffRoles.endAssignment(employee.getUser()) || current != null) {
            refreshTokenService.revokeAll(employee.getUser().getId());
        }
        return toResponse(employee, null);
    }

    @Override
    public EmployeeResponse getMine(Long userId) {
        currentUserLoader.loadWithAnyRole(userId, RoleName.STAFF, RoleName.BRANCH_MANAGER, RoleName.ADMIN);
        return toResponse(employeeRepository.findByUserId(userId).orElseThrow(() ->
                new BusinessException(ErrorCode.EMPLOYEE_NOT_FOUND, "Tài khoản này chưa có hồ sơ nhân viên")));
    }

    /** 400 when the manager label is used by an account that does not hold BRANCH_MANAGER. */
    private void rejectManagerLabel(Long userId, String position) {
        if (StaffRoles.isManagerPosition(position) && !staffRoles.isManager(userId)) {
            throw BusinessException.invalidField("positionAtStore",
                    "Muốn làm Quản lý chi nhánh, hãy nâng vai trò ở trang Người dùng & phân quyền");
        }
    }

    private void ensureAdmin(Long adminId) {
        currentUserLoader.loadWithAnyRole(adminId, RoleName.ADMIN);
    }

    private Employee lock(Long employeeId) {
        return employeeRepository.findByIdForUpdate(employeeId).orElseThrow(() -> notFound(employeeId));
    }

    private Optional<EmployeeAssignment> findCurrent(Long employeeId) {
        return assignmentRepository.findByEmployeeIdAndActiveTrueAndEndDateIsNull(employeeId);
    }

    private Store openStore(Integer storeId) {
        Store store = storeRepository.findById(storeId).orElseThrow(() -> new BusinessException(
                ErrorCode.STORE_NOT_FOUND, "Không tìm thấy chi nhánh id %d".formatted(storeId)));
        if (Boolean.FALSE.equals(store.getActive())) {
            throw BusinessException.invalidField("storeId", "Chi nhánh đang tạm đóng");
        }
        return store;
    }

    /**
     * Flushed at once: Hibernate runs inserts before updates, so without this the new open assignment would
     * be inserted while the old one is still open and uq_employee_assignments_active would refuse it.
     */
    private void close(EmployeeAssignment assignment) {
        assignment.setActive(false);
        assignment.setEndDate(today());
        assignmentRepository.saveAndFlush(assignment);
    }

    private EmployeeAssignment startAssignment(Employee employee, Store store, String positionAtStore, LocalDate start) {
        EmployeeAssignment assignment = new EmployeeAssignment();
        assignment.setEmployee(employee);
        assignment.setStore(store);
        assignment.setPositionAtStore(trimToNull(positionAtStore));
        assignment.setStartDate(start);
        assignment.setActive(true);
        return assignmentRepository.saveAndFlush(assignment);
    }

    private LocalDate today() {
        return LocalDate.now(clock);
    }

    private Map<Long, EmployeeAssignment> currentAssignments(List<Long> employeeIds) {
        if (employeeIds.isEmpty()) {
            return Map.of();
        }
        return assignmentRepository.findActiveWithStoreByEmployeeIdIn(employeeIds).stream()
                .collect(Collectors.toMap(a -> a.getEmployee().getId(), Function.identity()));
    }

    private EmployeeResponse toResponse(Employee employee) {
        return toResponse(employee, currentAssignments(List.of(employee.getId())).get(employee.getId()));
    }

    private static EmployeeResponse toResponse(Employee employee, EmployeeAssignment assignment) {
        return EmployeeMapper.toResponse(employee, assignment);
    }

    private static BusinessException notFound(Long employeeId) {
        return new BusinessException(ErrorCode.EMPLOYEE_NOT_FOUND, "Không tìm thấy nhân viên id %d".formatted(employeeId));
    }

    private static BusinessException duplicateCode(String code) {
        return new BusinessException(ErrorCode.DUPLICATE_EMPLOYEE_CODE, "Mã nhân viên %s đã được dùng".formatted(code));
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
