package com.example.Tech.service.store;

import com.example.Tech.entity.employee.EmployeeAssignment;
import com.example.Tech.entity.order.Order;
import com.example.Tech.entity.store.Store;
import com.example.Tech.entity.user.RoleName;
import com.example.Tech.entity.user.User;
import com.example.Tech.exception.BusinessException;
import com.example.Tech.exception.ErrorCode;
import com.example.Tech.repository.employee.EmployeeAssignmentRepository;
import com.example.Tech.repository.store.StoreRepository;
import com.example.Tech.repository.user.UserRoleRepository;
import com.example.Tech.service.user.CurrentUserLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StoreAccessGuardTest {

    @Mock
    private CurrentUserLoader currentUserLoader;

    @Mock
    private UserRoleRepository userRoleRepository;

    @Mock
    private EmployeeAssignmentRepository assignmentRepository;

    @Mock
    private StoreRepository storeRepository;

    @InjectMocks
    private StoreAccessGuard guard;

    @BeforeEach
    void setUp() {
        lenient().when(currentUserLoader.loadWithAnyRole(anyLong(), org.mockito.ArgumentMatchers.eq(RoleName.STAFF),
                org.mockito.ArgumentMatchers.eq(RoleName.ADMIN))).thenReturn(new User());
        lenient().when(storeRepository.findById(3)).thenReturn(Optional.of(store(3)));
        lenient().when(storeRepository.findById(4)).thenReturn(Optional.of(store(4)));
    }

    @Test
    void admin_mayActOnAnyStore_withoutAnAssignment() {
        when(userRoleRepository.findRoleNamesByUserId(1L)).thenReturn(List.of("ADMIN", "STAFF"));

        assertThat(guard.require(1L, 4).store().getId()).isEqualTo(4);
        verify(assignmentRepository, never()).findActiveStoreIdByUserId(1L);
    }

    @Test
    void staff_mayActOnTheirOwnStore() {
        when(userRoleRepository.findRoleNamesByUserId(2L)).thenReturn(List.of("STAFF"));
        when(assignmentRepository.findActiveStoreIdByUserId(2L)).thenReturn(Optional.of(3));

        assertThat(guard.require(2L, 3).store().getId()).isEqualTo(3);
    }

    @Test
    void staff_ofAnotherStore_isDenied() {
        when(userRoleRepository.findRoleNamesByUserId(2L)).thenReturn(List.of("STAFF"));
        when(assignmentRepository.findActiveStoreIdByUserId(2L)).thenReturn(Optional.of(3));

        assertThatThrownBy(() -> guard.require(2L, 4))
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.ACCESS_DENIED);
    }

    @Test
    void staff_withoutAnAssignment_isDenied() {
        when(userRoleRepository.findRoleNamesByUserId(2L)).thenReturn(List.of("STAFF"));
        when(assignmentRepository.findActiveStoreIdByUserId(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> guard.require(2L, 3))
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.NO_ACTIVE_STORE_ASSIGNMENT);
    }

    @Test
    void unknownStore_isNotFound() {
        when(storeRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> guard.require(1L, 99))
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.STORE_NOT_FOUND);
    }

    @Test
    void orderScope_admin_reachesEveryOrder_includingOrdersWithoutAStore() {
        when(userRoleRepository.findRoleNamesByUserId(1L)).thenReturn(List.of("ADMIN"));

        StoreAccessGuard.OrderScope scope = guard.orderScope(1L);

        assertThat(scope.admin()).isTrue();
        scope.check(order(store(4)));
        scope.check(order(null));
        verify(assignmentRepository, never()).findActiveStoreIdByUserId(1L);
    }

    @Test
    void orderScope_staff_onlyReachesTheOrdersOfTheirStore() {
        when(userRoleRepository.findRoleNamesByUserId(2L)).thenReturn(List.of("STAFF"));
        when(assignmentRepository.findActiveStoreIdByUserId(2L)).thenReturn(Optional.of(3));

        StoreAccessGuard.OrderScope scope = guard.orderScope(2L);

        assertThat(scope.storeId()).isEqualTo(3);
        scope.check(order(store(3)));
        for (Order other : List.of(order(store(4)), order(null))) {
            assertThatThrownBy(() -> scope.check(other))
                    .isInstanceOfSatisfying(BusinessException.class, ex -> {
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.ACCESS_DENIED);
                        assertThat(ex.getMessage()).isEqualTo("Đơn DH00000005 không thuộc chi nhánh của bạn");
                    });
        }
    }

    @Test
    void orderScope_staffWithoutAnAssignment_isDenied() {
        when(userRoleRepository.findRoleNamesByUserId(2L)).thenReturn(List.of("STAFF"));
        when(assignmentRepository.findActiveStoreIdByUserId(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> guard.orderScope(2L))
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.NO_ACTIVE_STORE_ASSIGNMENT);
    }

    @Test
    void processingScope_admin_isReadOnly_evenWithTheStaffRole() {
        when(userRoleRepository.findRoleNamesByUserId(1L)).thenReturn(List.of("ADMIN", "STAFF"));

        assertThatThrownBy(() -> guard.processingScope(1L))
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.ADMIN_READ_ONLY);
    }

    @Test
    void processingScope_staff_getsTheirStore() {
        when(userRoleRepository.findRoleNamesByUserId(2L)).thenReturn(List.of("STAFF"));
        when(assignmentRepository.findActiveStoreIdByUserId(2L)).thenReturn(Optional.of(3));

        assertThat(guard.processingScope(2L).storeId()).isEqualTo(3);
    }

    @Test
    void reportScope_admin_everyStore_branchManager_ownStore_otherStaff_denied() {
        when(userRoleRepository.findRoleNamesByUserId(1L)).thenReturn(List.of("ADMIN"));
        assertThat(guard.reportScope(1L).admin()).isTrue();

        when(userRoleRepository.findRoleNamesByUserId(2L)).thenReturn(List.of("STAFF"));
        when(assignmentRepository.findActiveWithStoreByUserId(2L)).thenReturn(Optional.of(assignment(3, "Quản lý chi nhánh")));
        assertThat(guard.reportScope(2L).storeId()).isEqualTo(3);

        when(assignmentRepository.findActiveWithStoreByUserId(2L)).thenReturn(Optional.of(assignment(3, "Thu ngân")));
        assertThatThrownBy(() -> guard.reportScope(2L))
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.ACCESS_DENIED);

        when(assignmentRepository.findActiveWithStoreByUserId(2L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> guard.reportScope(2L))
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.NO_ACTIVE_STORE_ASSIGNMENT);
    }

    private static EmployeeAssignment assignment(int storeId, String position) {
        EmployeeAssignment assignment = new EmployeeAssignment();
        assignment.setStore(store(storeId));
        assignment.setPositionAtStore(position);
        return assignment;
    }

    private static Order order(Store store) {
        Order order = new Order();
        order.setId(5L);
        order.setStore(store);
        return order;
    }

    private static Store store(int id) {
        Store store = new Store();
        store.setId(id);
        store.setName("Chi nhánh " + id);
        return store;
    }
}
