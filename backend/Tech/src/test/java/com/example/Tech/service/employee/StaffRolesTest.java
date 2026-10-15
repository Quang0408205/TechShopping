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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StaffRolesTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-10T03:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh"));

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private UserRoleRepository userRoleRepository;

    @Mock
    private EmployeeAssignmentRepository assignmentRepository;

    @Mock
    private StoreRepository storeRepository;

    private StaffRoles staffRoles;
    private User user;

    @BeforeEach
    void setUp() {
        staffRoles = new StaffRoles(roleRepository, userRoleRepository, assignmentRepository, storeRepository, CLOCK);
        user = new User();
        user.setId(7L);
    }

    private static Role role(int id, String name) {
        Role role = new Role(name, null);
        role.setId(id);
        return role;
    }

    @Test
    void managerPositionLabel_isRecognisedIgnoringCaseAndSpaces() {
        assertThat(StaffRoles.isManagerPosition(" quản lý chi nhánh ")).isTrue();
        assertThat(StaffRoles.isManagerPosition("Thu ngân")).isFalse();
        assertThat(StaffRoles.isManagerPosition(null)).isFalse();
    }

    @Test
    void freeManagerSeat_locksTheStore_andIgnoresTheSameAccount() {
        when(assignmentRepository.findActiveManagerUserIds(3)).thenReturn(List.of(7L));

        staffRoles.requireFreeManagerSeat(3, 7L);

        verify(storeRepository).findByIdForUpdate(3);
    }

    @Test
    void freeManagerSeat_anotherManagerAlreadyThere_isAConflict() {
        when(assignmentRepository.findActiveManagerUserIds(3)).thenReturn(List.of(9L));

        assertThatThrownBy(() -> staffRoles.requireFreeManagerSeat(3, 7L))
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.STORE_ALREADY_HAS_MANAGER);
    }

    @Test
    void setRole_dropsEveryOtherRole_andAddsTheTarget() {
        Role customer = role(1, "CUSTOMER");
        Role staff = role(2, "STAFF");
        Role manager = role(3, "BRANCH_MANAGER");
        UserRole oldCustomer = new UserRole(user, customer);
        UserRole oldStaff = new UserRole(user, staff);
        when(userRoleRepository.findAllByIdUserId(7L)).thenReturn(List.of(oldCustomer, oldStaff));
        when(roleRepository.findByName("BRANCH_MANAGER")).thenReturn(Optional.of(manager));

        staffRoles.setRole(user, RoleName.BRANCH_MANAGER);

        verify(userRoleRepository).delete(oldCustomer);
        verify(userRoleRepository).delete(oldStaff);
        ArgumentCaptor<UserRole> added = ArgumentCaptor.forClass(UserRole.class);
        verify(userRoleRepository).save(added.capture());
        assertThat(added.getValue().getRole()).isSameAs(manager);
    }

    @Test
    void setRole_keepsTheTargetWhenAlreadyHeld() {
        UserRole held = new UserRole(user, role(2, "STAFF"));
        UserRole customer = new UserRole(user, role(1, "CUSTOMER"));
        when(userRoleRepository.findAllByIdUserId(7L)).thenReturn(List.of(customer, held));

        staffRoles.setRole(user, RoleName.STAFF);

        verify(userRoleRepository).delete(customer);
        verify(userRoleRepository, never()).delete(held);
        verify(userRoleRepository, never()).save(any());
    }

    @Test
    void setRole_neverTouchesAnAdministrator() {
        when(userRoleRepository.findAllByIdUserId(7L)).thenReturn(List.of(new UserRole(user, role(4, "ADMIN"))));

        assertThatThrownBy(() -> staffRoles.setRole(user, RoleName.STAFF))
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.ACCESS_DENIED);
        verify(userRoleRepository, never()).delete(any());
    }

    @Test
    void endAssignment_ofAManager_closesIt_andGoesBackToStaff() {
        EmployeeAssignment assignment = new EmployeeAssignment();
        assignment.setActive(true);
        when(assignmentRepository.findActiveWithStoreByUserId(7L)).thenReturn(Optional.of(assignment));
        when(userRoleRepository.findRoleNamesByUserId(7L)).thenReturn(List.of("BRANCH_MANAGER"));
        when(userRoleRepository.findAllByIdUserId(7L)).thenReturn(List.of(new UserRole(user, role(3, "BRANCH_MANAGER"))));
        when(roleRepository.findByName("STAFF")).thenReturn(Optional.of(role(2, "STAFF")));

        assertThat(staffRoles.endAssignment(user)).isTrue();

        assertThat(assignment.getActive()).isFalse();
        assertThat(assignment.getEndDate()).isEqualTo(LocalDate.of(2026, 10, 10));
        verify(assignmentRepository).saveAndFlush(assignment);
        ArgumentCaptor<UserRole> added = ArgumentCaptor.forClass(UserRole.class);
        verify(userRoleRepository).save(added.capture());
        assertThat(added.getValue().getRole().getName()).isEqualTo("STAFF");
    }

    @Test
    void endAssignment_ofAnAccountWithoutOne_changesNothing() {
        when(assignmentRepository.findActiveWithStoreByUserId(7L)).thenReturn(Optional.empty());
        when(userRoleRepository.findRoleNamesByUserId(7L)).thenReturn(List.of("CUSTOMER"));

        assertThat(staffRoles.endAssignment(user)).isFalse();
        verify(userRoleRepository, never()).save(any());
        verify(assignmentRepository, never()).saveAndFlush(any());
    }
}
