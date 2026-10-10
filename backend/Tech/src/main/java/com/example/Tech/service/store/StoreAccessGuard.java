package com.example.Tech.service.store;

import com.example.Tech.entity.employee.EmployeeAssignment;
import com.example.Tech.entity.order.Order;
import com.example.Tech.entity.store.Store;
import com.example.Tech.entity.user.RoleName;
import com.example.Tech.entity.user.User;
import com.example.Tech.exception.BusinessException;
import com.example.Tech.exception.ErrorCode;
import com.example.Tech.mapper.order.OrderMapper;
import com.example.Tech.repository.employee.EmployeeAssignmentRepository;
import com.example.Tech.repository.store.StoreRepository;
import com.example.Tech.repository.user.UserRoleRepository;
import com.example.Tech.service.user.CurrentUserLoader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Store-scoped access for staff (Phase 7): an ADMIN may act on any store; a STAFF member only on the store of
 * their current open assignment (employee_assignments). Roles and the assignment are read from the database on
 * every call, so a token issued before a role change or a move to another store cannot reach the old store.
 */
@Component
@RequiredArgsConstructor
public class StoreAccessGuard {

    private final CurrentUserLoader currentUserLoader;
    private final UserRoleRepository userRoleRepository;
    private final EmployeeAssignmentRepository assignmentRepository;
    private final StoreRepository storeRepository;

    /** The caller and the store they may act on; 404 STORE_NOT_FOUND, 403 NO_ACTIVE_STORE_ASSIGNMENT / ACCESS_DENIED. */
    public Access require(Long userId, Integer storeId) {
        User user = currentUserLoader.loadWithAnyRole(userId, RoleName.STAFF, RoleName.ADMIN);
        Store store = storeRepository.findById(storeId).orElseThrow(() -> new BusinessException(
                ErrorCode.STORE_NOT_FOUND, "Không tìm thấy chi nhánh id %d".formatted(storeId)));
        if (userRoleRepository.findRoleNamesByUserId(userId).contains(RoleName.ADMIN.name())) {
            return new Access(user, store);
        }
        Integer ownStoreId = assignmentRepository.findActiveStoreIdByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NO_ACTIVE_STORE_ASSIGNMENT));
        if (!ownStoreId.equals(storeId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "Bạn chỉ được thao tác với chi nhánh của mình");
        }
        return new Access(user, store);
    }

    /**
     * Which orders the caller may see: every order for an ADMIN, otherwise only the orders of the STAFF member's
     * current store (403 NO_ACTIVE_STORE_ASSIGNMENT without one). Changes go through {@link #processingScope}.
     */
    public OrderScope orderScope(Long userId) {
        User user = currentUserLoader.loadWithAnyRole(userId, RoleName.STAFF, RoleName.ADMIN);
        if (userRoleRepository.findRoleNamesByUserId(userId).contains(RoleName.ADMIN.name())) {
            return new OrderScope(user, null);
        }
        Integer ownStoreId = assignmentRepository.findActiveStoreIdByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NO_ACTIVE_STORE_ASSIGNMENT));
        return new OrderScope(user, ownStoreId);
    }

    /**
     * Scope for changing an order, its payment / installment plan or an after-sales request: only STAFF of the
     * order's store. An ADMIN (even one who also has STAFF) only views and reassigns stores: 403 ADMIN_READ_ONLY.
     */
    public OrderScope processingScope(Long userId) {
        OrderScope scope = orderScope(userId);
        if (scope.admin()) {
            throw new BusinessException(ErrorCode.ADMIN_READ_ONLY);
        }
        return scope;
    }

    /**
     * Who may read sales reports: an ADMIN for every store (storeId null), or a STAFF member whose current position
     * is branch manager, for their store only (403 NO_ACTIVE_STORE_ASSIGNMENT / ACCESS_DENIED otherwise).
     */
    public OrderScope reportScope(Long userId) {
        User user = currentUserLoader.loadWithAnyRole(userId, RoleName.STAFF, RoleName.ADMIN);
        if (userRoleRepository.findRoleNamesByUserId(userId).contains(RoleName.ADMIN.name())) {
            return new OrderScope(user, null);
        }
        EmployeeAssignment assignment = assignmentRepository.findActiveWithStoreByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NO_ACTIVE_STORE_ASSIGNMENT));
        if (!EmployeeAssignment.BRANCH_MANAGER_POSITION.equals(assignment.getPositionAtStore())) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "Chỉ quản lý chi nhánh và quản trị viên xem được báo cáo");
        }
        return new OrderScope(user, assignment.getStore().getId());
    }

    public record Access(User user, Store store) {
    }

    /** {@code storeId} null = ADMIN, all stores. */
    public record OrderScope(User user, Integer storeId) {

        public boolean admin() {
            return storeId == null;
        }

        /** 403 ACCESS_DENIED when a STAFF member reaches an order of another store, or one without a store. */
        public void check(Order order) {
            if (admin()) {
                return;
            }
            Store store = order.getStore();
            if (store == null || !storeId.equals(store.getId())) {
                throw new BusinessException(ErrorCode.ACCESS_DENIED, "Đơn %s không thuộc chi nhánh của bạn"
                        .formatted(OrderMapper.code(order.getId())));
            }
        }
    }
}
