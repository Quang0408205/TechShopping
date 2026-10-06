package com.example.Tech.service.impl.employee;

import com.example.Tech.dto.request.employee.AssignmentRequest;
import com.example.Tech.dto.request.employee.EmployeeCreateRequest;
import com.example.Tech.dto.request.employee.EmployeeUpdateRequest;
import com.example.Tech.dto.response.employee.EmployeeResponse;
import com.example.Tech.entity.employee.Employee;
import com.example.Tech.entity.employee.EmployeeAssignment;
import com.example.Tech.entity.store.Store;
import com.example.Tech.entity.user.RoleName;
import com.example.Tech.entity.user.User;
import com.example.Tech.exception.BusinessException;
import com.example.Tech.exception.ErrorCode;
import com.example.Tech.repository.employee.EmployeeAssignmentRepository;
import com.example.Tech.repository.employee.EmployeeRepository;
import com.example.Tech.repository.store.StoreRepository;
import com.example.Tech.repository.user.UserRepository;
import com.example.Tech.repository.user.UserRoleRepository;
import com.example.Tech.service.user.CurrentUserLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceImplTest {

    private static final Long ADMIN_ID = 1L;
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 3);

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private EmployeeAssignmentRepository assignmentRepository;

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserRoleRepository userRoleRepository;

    @Mock
    private CurrentUserLoader currentUserLoader;

    private EmployeeServiceImpl service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-10-03T03:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh"));
        service = new EmployeeServiceImpl(employeeRepository, assignmentRepository, storeRepository, userRepository,
                userRoleRepository, currentUserLoader, clock);
        lenient().when(currentUserLoader.loadWithAnyRole(ADMIN_ID, RoleName.ADMIN)).thenReturn(new User());
        lenient().when(employeeRepository.save(any(Employee.class))).thenAnswer(invocation -> {
            Employee employee = invocation.getArgument(0);
            employee.setId(50L);
            return employee;
        });
        lenient().when(assignmentRepository.saveAndFlush(any(EmployeeAssignment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void create_accountWithoutStaffRole_isRejectedOnUserId() {
        when(userRepository.findById(10L)).thenReturn(Optional.of(user(10L)));
        when(userRoleRepository.findRoleNamesByUserId(10L)).thenReturn(List.of("CUSTOMER"));

        assertThatThrownBy(() -> service.create(ADMIN_ID, createRequest(10L, null, null, null)))
                .isInstanceOfSatisfying(BusinessException.class, ex -> {
                    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR);
                    assertThat(ex.getDetails()).containsKey("userId");
                });
        verify(employeeRepository, never()).save(any());
    }

    @Test
    void create_accountThatAlreadyHasAProfile_isAConflict() {
        staffAccount(10L);
        when(employeeRepository.existsByUserId(10L)).thenReturn(true);

        assertThatThrownBy(() -> service.create(ADMIN_ID, createRequest(10L, null, null, null)))
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.EMPLOYEE_ALREADY_EXISTS);
    }

    @Test
    void create_duplicateEmployeeCode_isAConflict() {
        staffAccount(10L);
        when(employeeRepository.existsByEmployeeCode("NV001")).thenReturn(true);

        assertThatThrownBy(() -> service.create(ADMIN_ID, createRequest(10L, " NV001 ", null, null)))
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.DUPLICATE_EMPLOYEE_CODE);
    }

    @Test
    void create_withAStore_startsTheAssignmentToday() {
        staffAccount(10L);
        when(storeRepository.findById(3)).thenReturn(Optional.of(store(3, true)));

        EmployeeResponse response = service.create(ADMIN_ID, createRequest(10L, "NV001", 3, " Quản lý chi nhánh "));

        assertThat(response.id()).isEqualTo(50L);
        assertThat(response.employeeCode()).isEqualTo("NV001");
        assertThat(response.assignment().storeId()).isEqualTo(3);
        assertThat(response.assignment().positionAtStore()).isEqualTo("Quản lý chi nhánh");
        assertThat(response.assignment().startDate()).isEqualTo(TODAY);
    }

    @Test
    void create_closedStore_isRejectedOnStoreId() {
        staffAccount(10L);
        when(storeRepository.findById(3)).thenReturn(Optional.of(store(3, false)));

        assertThatThrownBy(() -> service.create(ADMIN_ID, createRequest(10L, null, 3, null)))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getDetails()).containsKey("storeId"));
        verify(employeeRepository, never()).save(any());
    }

    @Test
    void assign_toAnotherStore_closesTheCurrentAssignmentBeforeStartingTheNewOne() {
        Employee employee = employee(50L, true);
        EmployeeAssignment current = assignment(employee, store(3, true));
        when(employeeRepository.findByIdForUpdate(50L)).thenReturn(Optional.of(employee));
        when(storeRepository.findById(4)).thenReturn(Optional.of(store(4, true)));
        when(assignmentRepository.findByEmployeeIdAndActiveTrueAndEndDateIsNull(50L)).thenReturn(Optional.of(current));

        EmployeeResponse response = service.assign(ADMIN_ID, 50L, new AssignmentRequest(4, "Nhân viên", null));

        assertThat(current.getActive()).isFalse();
        assertThat(current.getEndDate()).isEqualTo(TODAY);
        assertThat(response.assignment().storeId()).isEqualTo(4);
        ArgumentCaptor<EmployeeAssignment> saved = ArgumentCaptor.forClass(EmployeeAssignment.class);
        InOrder order = inOrder(assignmentRepository);
        order.verify(assignmentRepository, times(2)).saveAndFlush(saved.capture());
        assertThat(saved.getAllValues().get(0)).isSameAs(current);
        assertThat(saved.getAllValues().get(1).getStore().getId()).isEqualTo(4);
    }

    @Test
    void assign_toTheSameStore_onlyChangesThePosition() {
        Employee employee = employee(50L, true);
        EmployeeAssignment current = assignment(employee, store(3, true));
        when(employeeRepository.findByIdForUpdate(50L)).thenReturn(Optional.of(employee));
        when(storeRepository.findById(3)).thenReturn(Optional.of(store(3, true)));
        when(assignmentRepository.findByEmployeeIdAndActiveTrueAndEndDateIsNull(50L)).thenReturn(Optional.of(current));
        when(assignmentRepository.save(current)).thenReturn(current);

        service.assign(ADMIN_ID, 50L, new AssignmentRequest(3, "Quản lý chi nhánh", null));

        assertThat(current.getActive()).isTrue();
        assertThat(current.getPositionAtStore()).isEqualTo("Quản lý chi nhánh");
        verify(assignmentRepository, never()).saveAndFlush(any());
    }

    @Test
    void assign_employeeNoLongerWorking_isRejected() {
        when(employeeRepository.findByIdForUpdate(50L)).thenReturn(Optional.of(employee(50L, false)));

        assertThatThrownBy(() -> service.assign(ADMIN_ID, 50L, new AssignmentRequest(3, null, null)))
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
        verify(assignmentRepository, never()).saveAndFlush(any());
    }

    @Test
    void update_toInactive_endsTheCurrentAssignment() {
        Employee employee = employee(50L, true);
        EmployeeAssignment current = assignment(employee, store(3, true));
        when(employeeRepository.findByIdForUpdate(50L)).thenReturn(Optional.of(employee));
        when(assignmentRepository.findByEmployeeIdAndActiveTrueAndEndDateIsNull(50L)).thenReturn(Optional.of(current));

        EmployeeResponse response = service.update(ADMIN_ID, 50L,
                new EmployeeUpdateRequest(null, null, null, null, null, false));

        assertThat(response.active()).isFalse();
        assertThat(response.assignment()).isNull();
        assertThat(current.getActive()).isFalse();
        assertThat(current.getEndDate()).isEqualTo(TODAY);
    }

    @Test
    void getMine_withoutAProfile_isNotFound() {
        when(currentUserLoader.loadWithAnyRole(7L, RoleName.STAFF, RoleName.ADMIN)).thenReturn(user(7L));
        when(employeeRepository.findByUserId(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getMine(7L))
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.EMPLOYEE_NOT_FOUND);
    }

    private void staffAccount(Long userId) {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user(userId)));
        when(userRoleRepository.findRoleNamesByUserId(userId)).thenReturn(List.of("CUSTOMER", "STAFF"));
    }

    private static EmployeeCreateRequest createRequest(Long userId, String code, Integer storeId, String positionAtStore) {
        return new EmployeeCreateRequest(userId, code, null, null, null, null, storeId, positionAtStore);
    }

    private static User user(Long id) {
        User user = new User();
        user.setId(id);
        user.setUsername("u" + id);
        user.setEmail("u" + id + "@poy.vn");
        user.setFullname("Nhân Viên " + id);
        return user;
    }

    private static Employee employee(Long id, boolean active) {
        Employee employee = new Employee();
        employee.setId(id);
        employee.setUser(user(10L));
        employee.setActive(active);
        return employee;
    }

    private static Store store(int id, boolean active) {
        Store store = new Store();
        store.setId(id);
        store.setName("Chi nhánh " + id);
        store.setActive(active);
        return store;
    }

    private static EmployeeAssignment assignment(Employee employee, Store store) {
        EmployeeAssignment assignment = new EmployeeAssignment();
        assignment.setId(900L);
        assignment.setEmployee(employee);
        assignment.setStore(store);
        assignment.setStartDate(LocalDate.of(2026, 1, 1));
        assignment.setActive(true);
        return assignment;
    }
}
