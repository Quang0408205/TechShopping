package com.example.Tech.service.notification;

import com.example.Tech.dto.response.common.PageResponse;
import com.example.Tech.dto.response.notification.StaffNotificationResponse;
import com.example.Tech.entity.product.ProductVariant;
import com.example.Tech.entity.store.Store;
import com.example.Tech.exception.BusinessException;
import com.example.Tech.exception.ErrorCode;
import com.example.Tech.repository.notification.StaffNotificationRepository;
import com.example.Tech.service.store.StoreAccessGuard;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Internal notifications: services call {@link #publish} inside their own transaction (a failure here never breaks
 * the caller); staff read the notifications of their own store plus the shared ones, an ADMIN reads all of them.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StaffNotificationService {

    /** A variant at or below this many units is "low". */
    public static final int LOW_STOCK_MAX = 5;

    public enum Kind {
        NEW_ORDER,
        ORDER_CANCELLED,
        LOW_STOCK,
        SERVICE_REQUEST,
        STOCK_TRANSFER,
        PRODUCT_QUESTION
    }

    private final StaffNotificationRepository repository;
    private final StoreAccessGuard storeAccessGuard;

    /** {@code store} null = shown to every staff member. */
    @Transactional
    public void publish(Store store, Kind kind, String title, String message, String link) {
        try {
            repository.insert(store == null ? null : store.getId(), kind.name(), title, message, link);
        } catch (RuntimeException e) {
            log.warn("Could not save the {} notification: {}", kind, e.getMessage());
        }
    }

    /** Tells the store when a variant has just dropped to the low level, or to none. */
    @Transactional
    public void lowStock(Store store, ProductVariant variant, int before, int after) {
        boolean becameLow = before > LOW_STOCK_MAX && after <= LOW_STOCK_MAX;
        boolean ranOut = before > 0 && after <= 0;
        if (!becameLow && !ranOut) {
            return;
        }
        String name = variant.getProduct().getName() + " - " + variant.getVariantName();
        publish(store, Kind.LOW_STOCK, (after <= 0 ? "Hết hàng: " : "Sắp hết hàng: ") + name,
                "Chi nhánh %s còn %d sản phẩm".formatted(store.getName(), Math.max(after, 0)), "inventory.html");
    }

    public PageResponse<StaffNotificationResponse> list(Long userId, boolean unreadOnly, Pageable pageable) {
        Scope scope = scopeOf(userId);
        List<StaffNotificationResponse> rows = repository.page(userId, scope.all(), scope.storeId(), unreadOnly,
                pageable.getPageSize(), pageable.getOffset());
        long total = repository.count(userId, scope.all(), scope.storeId(), unreadOnly);
        return PageResponse.from(new PageImpl<>(rows, pageable, total));
    }

    public long unreadCount(Long userId) {
        Scope scope = scopeOf(userId);
        return repository.count(userId, scope.all(), scope.storeId(), true);
    }

    @Transactional
    public void markRead(Long userId, Long id) {
        Scope scope = scopeOf(userId);
        if (!repository.isVisible(id, scope.all(), scope.storeId())) {
            throw new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND);
        }
        repository.markRead(id, userId);
    }

    @Transactional
    public int markAllRead(Long userId) {
        Scope scope = scopeOf(userId);
        return repository.markAllRead(userId, scope.all(), scope.storeId());
    }

    /** 403 for non-staff; a staff member without a store sees only the shared notifications. */
    private Scope scopeOf(Long userId) {
        try {
            StoreAccessGuard.OrderScope scope = storeAccessGuard.orderScope(userId);
            return scope.admin() ? new Scope(true, -1) : new Scope(false, scope.storeId());
        } catch (BusinessException e) {
            if (e.getErrorCode() == ErrorCode.NO_ACTIVE_STORE_ASSIGNMENT) {
                return new Scope(false, -1);
            }
            throw e;
        }
    }

    private record Scope(boolean all, int storeId) {
    }
}
