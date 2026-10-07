package com.example.Tech.service.impl.order;

import com.example.Tech.entity.inventory.Inventory;
import com.example.Tech.entity.inventory.MovementType;
import com.example.Tech.entity.inventory.StockMovement;
import com.example.Tech.entity.order.DeliveryType;
import com.example.Tech.entity.order.Order;
import com.example.Tech.entity.order.OrderItem;
import com.example.Tech.entity.order.OrderStatus;
import com.example.Tech.entity.product.ProductVariant;
import com.example.Tech.entity.store.Store;
import com.example.Tech.entity.user.User;
import com.example.Tech.exception.BusinessException;
import com.example.Tech.exception.ErrorCode;
import com.example.Tech.mapper.order.OrderMapper;
import com.example.Tech.repository.inventory.InventoryRepository;
import com.example.Tech.repository.inventory.StockMovementRepository;
import com.example.Tech.repository.order.OrderItemRepository;
import com.example.Tech.repository.store.StoreRepository;
import com.example.Tech.service.order.NearestStoreResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Store side of an order's life (Phase 7), in the same shape as OrderPaymentLifecycle: no transaction of its own,
 * it runs inside the caller's, after the caller has locked the order row. At placement it decides which store
 * handles the order; stock is not checked there (an order can always be placed), only when staff confirm it.
 */
@Component
@RequiredArgsConstructor
public class OrderStockLifecycle {

    static final String PICKUP_ADDRESS_PREFIX = "Nhận tại cửa hàng: ";

    private final StoreRepository storeRepository;
    private final InventoryRepository inventoryRepository;
    private final StockMovementRepository stockMovementRepository;
    private final OrderItemRepository orderItemRepository;

    /** Rules bean validation cannot express; runs before the cart is read. */
    public void validateDeliverySelection(DeliveryType deliveryType, Integer pickupStoreId, String shippingAddress) {
        if (deliveryType == DeliveryType.PICKUP) {
            if (pickupStoreId == null) {
                throw BusinessException.invalidField("pickupStoreId", "Vui lòng chọn chi nhánh nhận hàng");
            }
            return;
        }
        if (pickupStoreId != null) {
            throw BusinessException.invalidField("pickupStoreId", "Chỉ chọn chi nhánh khi nhận hàng tại cửa hàng");
        }
        if (shippingAddress == null || shippingAddress.isBlank()) {
            throw BusinessException.invalidField("shippingAddress", "Vui lòng nhập địa chỉ giao hàng");
        }
    }

    /**
     * Sets the delivery type and the store of a new order. PICKUP: the chosen store, which must be open, and the
     * shipping address becomes the store's address. HOME_DELIVERY: the open store nearest to the shipping address
     * (NearestStoreResolver); with no open store at all the order is still placed, without a store.
     */
    public void assignBranch(Order order, DeliveryType deliveryType, Integer pickupStoreId) {
        order.setDeliveryType(deliveryType);
        if (deliveryType == DeliveryType.PICKUP) {
            Store store = storeRepository.findById(pickupStoreId)
                    .filter(found -> !Boolean.FALSE.equals(found.getActive()))
                    .orElseThrow(() -> BusinessException.invalidField("pickupStoreId",
                            "Chi nhánh không tồn tại hoặc đang tạm đóng"));
            order.setStore(store);
            order.setShippingAddress(PICKUP_ADDRESS_PREFIX + store.getName() + ", " + fullAddress(store));
            return;
        }
        Optional<Store> nearest = NearestStoreResolver.resolve(order.getShippingAddress(),
                storeRepository.findAllByActiveTrueOrderByName());
        order.setStore(nearest.orElse(null));
    }

    /**
     * PENDING → CONFIRMED: takes the order's quantities from its store. Locks the inventory rows of the store (by
     * variant id, like stock-in) after the caller has locked the order, checks every line first and only then deducts,
     * writing one OUT movement per variant. 409 ORDER_STORE_MISSING when the order has no store, 409 INSUFFICIENT_STOCK
     * listing every short variant (a variant never stocked there counts as 0).
     */
    public void onOrderConfirmed(Order order, User staff) {
        Store store = order.getStore();
        if (store == null) {
            throw new BusinessException(ErrorCode.ORDER_STORE_MISSING,
                    "Đơn %s chưa có chi nhánh xử lý; ADMIN cần gán chi nhánh trước khi xác nhận"
                            .formatted(OrderMapper.code(order.getId())));
        }
        Map<Long, Integer> needed = new TreeMap<>();
        Map<Long, ProductVariant> variants = new HashMap<>();
        for (OrderItem item : orderItemRepository.findAllWithProductByOrderIdIn(List.of(order.getId()))) {
            needed.merge(item.getVariant().getId(), item.getQuantity(), Integer::sum);
            variants.put(item.getVariant().getId(), item.getVariant());
        }
        Map<Long, Inventory> rows = lockRows(store.getId(), needed.keySet());

        List<String> missing = new ArrayList<>();
        needed.forEach((variantId, quantity) -> {
            int available = rows.containsKey(variantId) ? rows.get(variantId).getQuantity() : 0;
            if (available < quantity) {
                ProductVariant variant = variants.get(variantId);
                missing.add("%s - %s (cần %d, còn %d)".formatted(variant.getProduct().getName(),
                        variant.getVariantName(), quantity, available));
            }
        });
        if (!missing.isEmpty()) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK,
                    "Chi nhánh %s không đủ hàng: %s".formatted(store.getName(), String.join("; ", missing)));
        }

        needed.forEach((variantId, quantity) -> {
            Inventory row = rows.get(variantId);
            row.setQuantity(row.getQuantity() - quantity);
            stockMovementRepository.save(movement(order, store, variants.get(variantId), MovementType.OUT,
                    -quantity, staff));
        });
    }

    /**
     * CONFIRMED → CANCELLED gives back exactly what the confirmation took (its OUT movements), one RETURN movement per
     * variant. No-op for any other previous status, and for orders confirmed before stock was tracked (no OUT rows).
     */
    public void onOrderCancelled(Order order, OrderStatus from, User staff) {
        if (from != OrderStatus.CONFIRMED) {
            return;
        }
        List<StockMovement> taken = stockMovementRepository.findByOrderIdAndMovementTypeOrderById(order.getId(),
                MovementType.OUT);
        if (taken.isEmpty()) {
            return;
        }
        Store store = taken.getFirst().getStore();
        Map<Long, Integer> toReturn = new TreeMap<>();
        Map<Long, ProductVariant> variants = new HashMap<>();
        for (StockMovement out : taken) {
            toReturn.merge(out.getVariant().getId(), -out.getQuantityChange(), Integer::sum);
            variants.put(out.getVariant().getId(), out.getVariant());
        }
        Map<Long, Inventory> rows = lockRows(store.getId(), toReturn.keySet());
        toReturn.forEach((variantId, quantity) -> {
            Inventory row = rows.get(variantId);
            row.setQuantity(row.getQuantity() + quantity);
            stockMovementRepository.save(movement(order, store, variants.get(variantId), MovementType.RETURN,
                    quantity, staff));
        });
    }

    /**
     * ADMIN moves a PENDING order to another open store (e.g. after 409 INSUFFICIENT_STOCK). A pickup order's address
     * follows the new store, so the customer sees where to collect it. 404 STORE_NOT_FOUND, 400 details.storeId when
     * the store is closed.
     */
    public void reassignStore(Order order, Integer storeId) {
        Store store = storeRepository.findById(storeId).orElseThrow(() -> new BusinessException(
                ErrorCode.STORE_NOT_FOUND, "Không tìm thấy chi nhánh id %d".formatted(storeId)));
        if (Boolean.FALSE.equals(store.getActive())) {
            throw BusinessException.invalidField("storeId", "Chi nhánh đang tạm đóng");
        }
        order.setStore(store);
        if (order.getDeliveryType() == DeliveryType.PICKUP) {
            order.setShippingAddress(PICKUP_ADDRESS_PREFIX + store.getName() + ", " + fullAddress(store));
        }
    }

    private Map<Long, Inventory> lockRows(Integer storeId, Collection<Long> variantIds) {
        Map<Long, Inventory> rows = new HashMap<>();
        for (Inventory row : inventoryRepository.findAllForUpdate(storeId, List.copyOf(variantIds))) {
            rows.put(row.getId().getVariantId(), row);
        }
        return rows;
    }

    private static StockMovement movement(Order order, Store store, ProductVariant variant, MovementType type,
                                          int change, User staff) {
        StockMovement movement = new StockMovement();
        movement.setStore(store);
        movement.setVariant(variant);
        movement.setMovementType(type);
        movement.setQuantityChange(change);
        movement.setOrder(order);
        movement.setCreatedBy(staff);
        return movement;
    }

    private static String fullAddress(Store store) {
        StringBuilder text = new StringBuilder(store.getAddress());
        if (store.getDistrict() != null && !store.getDistrict().isBlank()) {
            text.append(", ").append(store.getDistrict());
        }
        if (store.getCity() != null && !store.getCity().isBlank()) {
            text.append(", ").append(store.getCity());
        }
        return text.toString();
    }
}
