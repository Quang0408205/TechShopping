package com.example.Tech.service.impl.user;

import com.example.Tech.dto.request.user.UserRolesUpdateRequest;
import com.example.Tech.dto.request.user.UserSearchRequest;
import com.example.Tech.dto.request.user.UserStatusUpdateRequest;
import com.example.Tech.dto.response.common.PageResponse;
import com.example.Tech.dto.response.user.UserResponse;
import com.example.Tech.entity.employee.EmployeeAssignment;
import com.example.Tech.entity.store.Store;
import com.example.Tech.entity.user.RoleName;
import com.example.Tech.entity.user.User;
import com.example.Tech.exception.BusinessException;
import com.example.Tech.exception.ErrorCode;
import com.example.Tech.mapper.user.UserMapper;
import com.example.Tech.repository.employee.EmployeeAssignmentRepository;
import com.example.Tech.repository.user.UserRepository;
import com.example.Tech.repository.user.UserRoleName;
import com.example.Tech.repository.user.UserRoleRepository;
import com.example.Tech.security.RefreshTokenService;
import com.example.Tech.service.employee.StaffRoles;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceImplTest {

    private static final Long ADMIN_ID = 1L;
    private static final Long USER_ID = 2L;
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-25T10:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh"));

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmployeeAssignmentRepository assignmentRepository;

    @Mock
    private StaffRoles staffRoles;

    @Mock
    private UserRoleRepository userRoleRepository;

    @Mock
    private RefreshTokenService refreshTokenService;

    private AdminUserServiceImpl service;

    private User admin;
    private User target;

    @BeforeEach
    void setUp() {
        service = new AdminUserServiceImpl(userRepository, assignmentRepository, staffRoles, userRoleRepository,
                refreshTokenService, new UserMapper(), CLOCK);
        admin = user(ADMIN_ID, "admin");
        target = user(USER_ID, "an.nguyen");
    }

    private static User user(Long id, String username) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setEmail(username + "@example.com");
        user.setFullname(username);
        return user;
    }

    private static EmployeeAssignment assignment(int storeId, String position) {
        Store store = new Store();
        store.setId(storeId);
        EmployeeAssignment assignment = new EmployeeAssignment();
        assignment.setStore(store);
        assignment.setPositionAtStore(position);
        return assignment;
    }

    /** The caller exists, is enabled and has ADMIN in the database. */
    private void stubActingAdmin() {
        when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(admin));
        when(userRoleRepository.findRoleNamesByUserId(ADMIN_ID)).thenReturn(List.of("ADMIN"));
    }

    private void stubTarget(String... roles) {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(target));
        when(userRoleRepository.findRoleNamesByUserId(USER_ID)).thenReturn(List.of(roles));
    }

    private static void assertError(ThrowingCallable call, ErrorCode expected) {
        assertThatThrownBy(call)
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(expected);
    }

    // ---------- acting admin re-check ----------

    @Test
    void callerNoLongerAdminInDatabase_isDenied() {
        when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(admin));
        when(userRoleRepository.findRoleNamesByUserId(ADMIN_ID)).thenReturn(List.of("CUSTOMER"));

        assertError(() -> service.getById(ADMIN_ID, USER_ID), ErrorCode.ACCESS_DENIED);
    }

    @Test
    void disabledOrMissingCaller_isRejected() {
        admin.setActive(false);
        when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(admin));
        assertError(() -> service.getById(ADMIN_ID, USER_ID), ErrorCode.ACCOUNT_DISABLED);

        when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.empty());
        assertError(() -> service.getById(ADMIN_ID, USER_ID), ErrorCode.INVALID_TOKEN);
    }

    // ---------- search / get ----------

    @Test
    @SuppressWarnings("unchecked")
    void search_loadsRolesOfThePageInOneQuery() {
        stubActingAdmin();
        Pageable pageable = PageRequest.of(0, 20);
        when(userRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(admin, target), pageable, 2));
        when(userRoleRepository.findRoleNamesByUserIds(any(Collection.class))).thenReturn(List.of(
                roleName(ADMIN_ID, "ADMIN"), roleName(USER_ID, "CUSTOMER"), roleName(USER_ID, "STAFF")));

        PageResponse<UserResponse> page = service.search(ADMIN_ID, new UserSearchRequest(null, null, null, null), pageable);

        assertThat(page.totalElements()).isEqualTo(2);
        assertThat(page.content().get(0).roles()).containsExactly("ADMIN");
        assertThat(page.content().get(1).roles()).containsExactly("CUSTOMER", "STAFF");
        verify(userRoleRepository).findRoleNamesByUserIds(List.of(ADMIN_ID, USER_ID));
    }

    private static UserRoleName roleName(Long userId, String name) {
        return new UserRoleName() {
            @Override
            public Long getUserId() {
                return userId;
            }

            @Override
            public String getRoleName() {
                return name;
            }
        };
    }

    @Test
    void getById_unknownUser_throwsNotFound() {
        stubActingAdmin();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertError(() -> service.getById(ADMIN_ID, USER_ID), ErrorCode.USER_NOT_FOUND);
    }

    // ---------- status ----------

    @Test
    void deactivate_setsInactive_andRevokesAllSessions() {
        stubActingAdmin();
        stubTarget("CUSTOMER");
        when(userRepository.saveAndFlush(target)).thenReturn(target);

        UserResponse response = service.updateStatus(ADMIN_ID, USER_ID, new UserStatusUpdateRequest(false));

        assertThat(response.isActive()).isFalse();
        verify(refreshTokenService).revokeAll(USER_ID);
        verify(staffRoles).endAssignment(target);
    }

    @Test
    void activate_doesNotRevokeSessions() {
        stubActingAdmin();
        stubTarget("CUSTOMER");
        target.setActive(false);
        when(userRepository.saveAndFlush(target)).thenReturn(target);

        assertThat(service.updateStatus(ADMIN_ID, USER_ID, new UserStatusUpdateRequest(true)).isActive()).isTrue();
        verify(refreshTokenService, never()).revokeAll(anyLong());
    }

    @Test
    void status_ofOwnAccount_isRefused() {
        stubActingAdmin();

        assertError(() -> service.updateStatus(ADMIN_ID, ADMIN_ID, new UserStatusUpdateRequest(false)),
                ErrorCode.CANNOT_MODIFY_OWN_ACCOUNT);
    }

    @Test
    void status_ofDeletedUser_isRefused() {
        stubActingAdmin();
        target.setDeletedAt(LocalDateTime.now());
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(target));

        assertError(() -> service.updateStatus(ADMIN_ID, USER_ID, new UserStatusUpdateRequest(true)),
                ErrorCode.USER_DELETED);
    }

    @Test
    void deactivate_lastEnabledAdmin_isRefused() {
        stubActingAdmin();
        stubTarget("ADMIN");
        when(userRoleRepository.countEnabledUsersWithRole("ADMIN")).thenReturn(1L);

        assertError(() -> service.updateStatus(ADMIN_ID, USER_ID, new UserStatusUpdateRequest(false)),
                ErrorCode.LAST_ADMIN);
        verify(userRepository, never()).saveAndFlush(any());
    }

    // ---------- roles ----------

    @Test
    void updateRoles_customerAdminOrSeveralRoles_areNotAssignable() {
        stubActingAdmin();

        for (List<String> roles : List.of(List.of("CUSTOMER"), List.of("ADMIN"), List.of("STAFF", "BRANCH_MANAGER"),
                List.of("STAFF", "CUSTOMER"), List.of("superuser"))) {
            assertError(() -> service.updateRoles(ADMIN_ID, USER_ID, new UserRolesUpdateRequest(roles)),
                    ErrorCode.ROLE_NOT_ASSIGNABLE);
        }
        verify(staffRoles, never()).setRole(any(), any());
    }

    @Test
    void updateRoles_customerAccount_isNotEligible() {
        stubActingAdmin();
        stubTarget("CUSTOMER");

        assertError(() -> service.updateRoles(ADMIN_ID, USER_ID, new UserRolesUpdateRequest(List.of("STAFF"))),
                ErrorCode.CUSTOMER_ACCOUNT_NOT_ELIGIBLE);
        verify(staffRoles, never()).setRole(any(), any());
    }

    @Test
    void updateRoles_adminAccounts_areUntouchable() {
        stubActingAdmin();
        stubTarget("ADMIN");
        assertError(() -> service.updateRoles(ADMIN_ID, USER_ID, new UserRolesUpdateRequest(List.of("STAFF"))),
                ErrorCode.ACCESS_DENIED);

        assertError(() -> service.updateRoles(ADMIN_ID, ADMIN_ID, new UserRolesUpdateRequest(List.of("STAFF"))),
                ErrorCode.CANNOT_MODIFY_OWN_ACCOUNT);
        verify(staffRoles, never()).setRole(any(), any());
    }

    @Test
    void updateRoles_promoteToManager_needsAFreeSeatInTheirStore_thenRevokesSessions() {
        stubActingAdmin();
        stubTarget("STAFF");
        EmployeeAssignment assignment = assignment(3, "Nhân viên bán hàng");
        when(assignmentRepository.findActiveWithStoreByUserId(USER_ID)).thenReturn(Optional.of(assignment));

        UserResponse response = service.updateRoles(ADMIN_ID, USER_ID, new UserRolesUpdateRequest(List.of(" branch_manager ")));

        assertThat(response.roles()).containsExactly("BRANCH_MANAGER");
        assertThat(assignment.getPositionAtStore()).isEqualTo(EmployeeAssignment.BRANCH_MANAGER_POSITION);
        verify(staffRoles).requireFreeManagerSeat(3, USER_ID);
        verify(staffRoles).setRole(target, RoleName.BRANCH_MANAGER);
        verify(refreshTokenService).revokeAll(USER_ID);
    }

    @Test
    void updateRoles_promoteWithoutAssignment_isRefused() {
        stubActingAdmin();
        stubTarget("STAFF");
        when(assignmentRepository.findActiveWithStoreByUserId(USER_ID)).thenReturn(Optional.empty());

        assertError(() -> service.updateRoles(ADMIN_ID, USER_ID, new UserRolesUpdateRequest(List.of("BRANCH_MANAGER"))),
                ErrorCode.MANAGER_REQUIRES_ASSIGNMENT);
        verify(staffRoles, never()).setRole(any(), any());
        verify(refreshTokenService, never()).revokeAll(anyLong());
    }

    @Test
    void updateRoles_promoteIntoAStoreThatHasAManager_isAConflict() {
        stubActingAdmin();
        stubTarget("STAFF");
        EmployeeAssignment assignment = assignment(3, "Thu ngân");
        when(assignmentRepository.findActiveWithStoreByUserId(USER_ID)).thenReturn(Optional.of(assignment));
        doThrow(new BusinessException(ErrorCode.STORE_ALREADY_HAS_MANAGER)).when(staffRoles).requireFreeManagerSeat(3, USER_ID);

        assertError(() -> service.updateRoles(ADMIN_ID, USER_ID, new UserRolesUpdateRequest(List.of("BRANCH_MANAGER"))),
                ErrorCode.STORE_ALREADY_HAS_MANAGER);
        assertThat(assignment.getPositionAtStore()).isEqualTo("Thu ngân");
        verify(staffRoles, never()).setRole(any(), any());
        verify(refreshTokenService, never()).revokeAll(anyLong());
    }

    @Test
    void updateRoles_demoteManager_resetsTheLabel_andRevokesSessions() {
        stubActingAdmin();
        stubTarget("BRANCH_MANAGER");
        EmployeeAssignment assignment = assignment(3, EmployeeAssignment.BRANCH_MANAGER_POSITION);
        when(assignmentRepository.findActiveWithStoreByUserId(USER_ID)).thenReturn(Optional.of(assignment));

        UserResponse response = service.updateRoles(ADMIN_ID, USER_ID, new UserRolesUpdateRequest(List.of("STAFF")));

        assertThat(response.roles()).containsExactly("STAFF");
        assertThat(assignment.getPositionAtStore()).isEqualTo(StaffRoles.DEFAULT_POSITION);
        verify(staffRoles).setRole(target, RoleName.STAFF);
        verify(refreshTokenService).revokeAll(USER_ID);
    }

    @Test
    void updateRoles_sameRole_changesNothing() {
        stubActingAdmin();
        stubTarget("STAFF");

        assertThat(service.updateRoles(ADMIN_ID, USER_ID, new UserRolesUpdateRequest(List.of(" staff "))).roles())
                .containsExactly("STAFF");
        verify(staffRoles, never()).setRole(any(), any());
        verify(refreshTokenService, never()).revokeAll(anyLong());
    }

    @Test
    void updateRoles_legacyStaffWithCustomer_isNormalisedToOneRole() {
        stubActingAdmin();
        stubTarget("CUSTOMER", "STAFF");
        when(assignmentRepository.findActiveWithStoreByUserId(USER_ID)).thenReturn(Optional.empty());

        service.updateRoles(ADMIN_ID, USER_ID, new UserRolesUpdateRequest(List.of("STAFF")));

        verify(staffRoles).setRole(target, RoleName.STAFF);
    }

    // ---------- delete ----------

    @Test
    void delete_softDeletes_andRevokesAllSessions() {
        stubActingAdmin();
        stubTarget("CUSTOMER");

        service.delete(ADMIN_ID, USER_ID);

        assertThat(target.getDeletedAt()).isEqualTo(LocalDateTime.of(2026, 9, 25, 17, 0));
        assertThat(target.getActive()).isFalse();
        verify(userRepository).saveAndFlush(target);
        verify(refreshTokenService).revokeAll(USER_ID);
        verify(staffRoles).endAssignment(target);
    }

    @Test
    void delete_alreadyDeleted_isANoOp() {
        stubActingAdmin();
        target.setDeletedAt(LocalDateTime.now());
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(target));

        service.delete(ADMIN_ID, USER_ID);

        verify(userRepository, never()).saveAndFlush(any());
        verify(refreshTokenService, never()).revokeAll(anyLong());
    }

    @Test
    void delete_ownAccount_isRefused() {
        stubActingAdmin();

        assertError(() -> service.delete(ADMIN_ID, ADMIN_ID), ErrorCode.CANNOT_MODIFY_OWN_ACCOUNT);
    }

    @Test
    void delete_lastEnabledAdmin_isRefused() {
        stubActingAdmin();
        stubTarget("ADMIN");
        when(userRoleRepository.countEnabledUsersWithRole("ADMIN")).thenReturn(1L);

        assertError(() -> service.delete(ADMIN_ID, USER_ID), ErrorCode.LAST_ADMIN);
    }
}
