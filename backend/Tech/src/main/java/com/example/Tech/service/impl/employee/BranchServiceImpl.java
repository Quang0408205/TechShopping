package com.example.Tech.service.impl.employee;

import com.example.Tech.dto.request.employee.BranchEmployeeUpdateRequest;
import com.example.Tech.dto.request.employee.EmployeeSearchRequest;
import com.example.Tech.dto.response.common.PageResponse;
import com.example.Tech.dto.response.employee.BranchResponse;
import com.example.Tech.dto.response.employee.EmployeeResponse;
import com.example.Tech.entity.employee.Employee;
import com.example.Tech.entity.employee.EmployeeAssignment;
import com.example.Tech.entity.store.Store;
import com.example.Tech.entity.user.RoleName;
import com.example.Tech.exception.BusinessException;
import com.example.Tech.exception.ErrorCode;
import com.example.Tech.mapper.employee.EmployeeMapper;
import com.example.Tech.repository.employee.EmployeeAssignmentRepository;
import com.example.Tech.repository.employee.EmployeeFilterSpecifications;
import com.example.Tech.repository.employee.EmployeeRepository;
import com.example.Tech.repository.user.UserRepository;
import com.example.Tech.security.RefreshTokenService;
import com.example.Tech.service.employee.BranchService;
import com.example.Tech.service.employee.StaffRoles;
import com.example.Tech.service.user.CurrentUserLoader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BranchServiceImpl implements BranchService {

    private final CurrentUserLoader currentUserLoader;
    private final EmployeeRepository employeeRepository;
    private final EmployeeAssignmentRepository assignmentRepository;
    private final UserRepository userRepository;
    private final StaffRoles staffRoles;
    private final RefreshTokenService refreshTokenService;
    private final Clock clock;

    @Override
    public BranchResponse me(Long managerId) {
        Store store = ownAssignment(managerId).getStore();
        return new BranchResponse(store.getId(), store.getName(), store.getAddress(), store.getPhone(),
                store.getCity(), store.getDistrict(), !Boolean.FALSE.equals(store.getActive()),
                assignmentRepository.countWorkingEmployees(store.getId()));
    }

    @Override
    public PageResponse<EmployeeResponse> employees(Long managerId, EmployeeSearchRequest filter, Pageable pageable) {
        Integer storeId = ownAssignment(managerId).getStore().getId();
        // the store filter of the request is ignored: a manager only ever sees their own branch
        Specification<Employee> spec = Specification.allOf(
                EmployeeFilterSpecifications.matching(new EmployeeSearchRequest(filter.keyword(), null, filter.active())),
                EmployeeFilterSpecifications.latestAssignmentAt(storeId));
        Page<Employee> page = employeeRepository.findAll(spec, pageable);
        List<Long> ids = page.getContent().stream().map(Employee::getId).toList();
        Map<Long, EmployeeAssignment> current = ids.isEmpty() ? Map.of()
                : assignmentRepository.findActiveWithStoreByEmployeeIdIn(ids).stream()
                .collect(Collectors.toMap(a -> a.getEmployee().getId(), Function.identity()));
        return PageResponse.from(page.map(employee -> {
            EmployeeResponse response = EmployeeMapper.toResponse(employee, current.get(employee.getId()));
            // another manager's pay is not shown (the caller's own row stays complete)
            boolean otherManager = !employee.getUser().getId().equals(managerId)
                    && staffRoles.isManager(employee.getUser().getId());
            return otherManager ? withoutSalary(response) : response;
        }));
    }

    @Override
    @Transactional
    public EmployeeResponse updateEmployee(Long managerId, Long employeeId, BranchEmployeeUpdateRequest request) {
        EmployeeAssignment current = requireOwnStaff(managerId, employeeId);
        Employee employee = current.getEmployee();
        String code = trimToNull(request.employeeCode());
        if (code != null && employeeRepository.existsByEmployeeCodeAndIdNot(code, employeeId)) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMPLOYEE_CODE, "Mã nhân viên %s đã được dùng".formatted(code));
        }
        String label = trimToNull(request.positionAtStore());
        if (StaffRoles.isManagerPosition(label)) {
            throw BusinessException.invalidField("positionAtStore",
                    "Chức danh Quản lý chi nhánh chỉ dành cho vai trò Quản lý chi nhánh");
        }
        employee.setEmployeeCode(code);
        employee.setDepartment(trimToNull(request.department()));
        employee.setPosition(trimToNull(request.position()));
        employee.setSalary(request.salary());
        employee.setHiringDate(request.hiringDate());
        if (label != null) {
            current.setPositionAtStore(label);
            assignmentRepository.save(current);
        }
        employeeRepository.saveAndFlush(employee);
        log.info("Manager id={} updated employee id={}", managerId, employeeId);
        return EmployeeMapper.toResponse(employee, current);
    }

    @Override
    @Transactional
    public EmployeeResponse deactivateEmployee(Long managerId, Long employeeId) {
        EmployeeAssignment current = requireOwnStaff(managerId, employeeId);
        Employee employee = current.getEmployee();
        current.setActive(false);
        current.setEndDate(LocalDate.now(clock));
        assignmentRepository.saveAndFlush(current);
        employee.setActive(false);
        employeeRepository.saveAndFlush(employee);
        employee.getUser().setActive(false);
        userRepository.saveAndFlush(employee.getUser());
        refreshTokenService.revokeAll(employee.getUser().getId());
        log.info("Manager id={} let employee id={} go (account locked)", managerId, employeeId);
        return EmployeeMapper.toResponse(employee, null);
    }

    /** The manager's role and current store, both read from the database. */
    private EmployeeAssignment ownAssignment(Long managerId) {
        currentUserLoader.loadWithAnyRole(managerId, RoleName.BRANCH_MANAGER);
        return assignmentRepository.findActiveWithStoreByUserId(managerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NO_ACTIVE_STORE_ASSIGNMENT));
    }

    /**
     * The current assignment of a working STAFF member of the manager's own store (employee row locked).
     * 404 unknown id; 403 for the manager themselves, another manager, or someone at another store.
     */
    private EmployeeAssignment requireOwnStaff(Long managerId, Long employeeId) {
        Integer storeId = ownAssignment(managerId).getStore().getId();
        Employee employee = employeeRepository.findByIdForUpdate(employeeId).orElseThrow(() ->
                new BusinessException(ErrorCode.EMPLOYEE_NOT_FOUND, "Không tìm thấy nhân viên id %d".formatted(employeeId)));
        if (employee.getUser().getId().equals(managerId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "Bạn không tự thay đổi hồ sơ của chính mình");
        }
        EmployeeAssignment current = assignmentRepository.findByEmployeeIdAndActiveTrueAndEndDateIsNull(employeeId)
                .filter(assignment -> assignment.getStore().getId().equals(storeId))
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCESS_DENIED,
                        "Nhân viên không thuộc chi nhánh của bạn"));
        if (staffRoles.isManager(employee.getUser().getId())) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "Không thay đổi hồ sơ của quản lý khác");
        }
        if (Boolean.FALSE.equals(employee.getActive())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Nhân viên đã nghỉ");
        }
        return current;
    }

    private static EmployeeResponse withoutSalary(EmployeeResponse r) {
        return new EmployeeResponse(r.id(), r.userId(), r.username(), r.email(), r.fullname(), r.phone(),
                r.employeeCode(), r.department(), r.position(), null, r.hiringDate(), r.active(), r.assignment(),
                r.createdAt(), r.updatedAt());
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
