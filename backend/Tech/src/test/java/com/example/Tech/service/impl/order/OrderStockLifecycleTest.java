package com.example.Tech.service.impl.order;

import com.example.Tech.entity.inventory.Inventory;
import com.example.Tech.entity.inventory.MovementType;
import com.example.Tech.entity.inventory.StockMovement;
import com.example.Tech.entity.order.DeliveryType;
import com.example.Tech.entity.order.Order;
import com.example.Tech.entity.order.OrderItem;
import com.example.Tech.entity.order.OrderStatus;
import com.example.Tech.entity.product.Product;
import com.example.Tech.entity.product.ProductVariant;
import com.example.Tech.entity.store.Store;
import com.example.Tech.entity.user.User;
import com.example.Tech.exception.BusinessException;
import com.example.Tech.exception.ErrorCode;
import com.example.Tech.repository.inventory.InventoryRepository;
import com.example.Tech.repository.inventory.StockMovementRepository;
import com.example.Tech.repository.order.OrderItemRepository;
import com.example.Tech.repository.store.StoreRepository;
import com.example.Tech.service.inventory.StockAvailability;
import com.example.Tech.service.inventory.StockAvailability.StoreStock;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderStockLifecycleTest {

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private StockMovementRepository stockMovementRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private StockAvailability stockAvailability;

    @InjectMocks
    private OrderStockLifecycle lifecycle;

    @Test
    void validate_pickupNeedsAStore_homeDeliveryNeedsAnAddressAndNoStore() {
        assertThatCode(() -> lifecycle.validateDeliverySelection(DeliveryType.PICKUP, 3, null)).doesNotThrowAnyException();
        assertThatCode(() -> lifecycle.validateDeliverySelection(DeliveryType.HOME_DELIVERY, null, "Quận 1"))
                .doesNotThrowAnyException();

        assertInvalid(DeliveryType.PICKUP, null, "Quận 1", "pickupStoreId");
        assertInvalid(DeliveryType.HOME_DELIVERY, 3, "Quận 1", "pickupStoreId");
        assertInvalid(DeliveryType.HOME_DELIVERY, null, "   ", "shippingAddress");
        assertInvalid(DeliveryType.HOME_DELIVERY, null, null, "shippingAddress");
    }

    // One open store must hold the whole order; variant 10 × 2 and variant 20 × 1 below
    private static final Map<Long, Integer> NEEDED = Map.of(10L, 2, 20L, 1);
    private static final Map<Long, String> LABELS = Map.of(10L, "Điện thoại - Đen", 20L, "Ốp lưng - Trong");

    @Test
    void assignBranch_pickup_usesTheChosenStoreAndItsAddress_whenItHoldsEverything() {
        Store store = store(3, "POY Quận 1", true);
        when(storeRepository.findById(3)).thenReturn(Optional.of(store));
        when(stockAvailability.byStore(NEEDED.keySet()))
                .thenReturn(Map.of(3, new StoreStock(store, Map.of(10L, 2, 20L, 5))));
        Order order = new Order();

        lifecycle.assignBranch(order, DeliveryType.PICKUP, 3, NEEDED, LABELS);

        assertThat(order.getStore()).isSameAs(store);
        assertThat(order.getDeliveryType()).isEqualTo(DeliveryType.PICKUP);
        assertThat(order.getShippingAddress())
                .isEqualTo("Nhận tại cửa hàng: POY Quận 1, 12 Nguyễn Huệ, Quận 1, Hồ Chí Minh");
    }

    @Test
    void assignBranch_pickupAtAStoreLackingItems_isOutOfStock_listingEveryShortLine() {
        Store store = store(3, "POY Quận 1", true);
        when(storeRepository.findById(3)).thenReturn(Optional.of(store));
        when(stockAvailability.byStore(NEEDED.keySet())).thenReturn(Map.of(3, new StoreStock(store, Map.of(10L, 1))));
        Order order = new Order();

        assertThatThrownBy(() -> lifecycle.assignBranch(order, DeliveryType.PICKUP, 3, NEEDED, LABELS))
                .isInstanceOfSatisfying(BusinessException.class, ex -> {
                    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.OUT_OF_STOCK);
                    assertThat(ex.getMessage()).isEqualTo("Chi nhánh POY Quận 1 không đủ hàng: Điện thoại - Đen (cần 2, còn 1); "
                            + "Ốp lưng - Trong (cần 1, còn 0). Hãy chọn chi nhánh khác hoặc giao tận nhà");
                });
        assertThat(order.getStore()).isNull();
    }

    @Test
    void assignBranch_pickupAtAClosedOrUnknownStore_isRejectedOnPickupStoreId() {
        when(storeRepository.findById(3)).thenReturn(Optional.of(store(3, "Đóng", false)));
        when(storeRepository.findById(4)).thenReturn(Optional.empty());

        for (int id : new int[]{3, 4}) {
            assertThatThrownBy(() -> lifecycle.assignBranch(new Order(), DeliveryType.PICKUP, id, NEEDED, LABELS))
                    .isInstanceOfSatisfying(BusinessException.class,
                            ex -> assertThat(ex.getDetails()).containsKey("pickupStoreId"));
        }
    }

    @Test
    void assignBranch_homeDelivery_picksTheNearestStoreAmongThoseHoldingTheWholeOrder() {
        Store q1 = store(1, "POY Quận 1", true);
        Store q5 = store(2, "POY Quận 5", true);
        q5.setDistrict("Quận 5");
        Order order = new Order();
        order.setShippingAddress("12 Nguyễn Trãi, Quận 5, TP. Hồ Chí Minh");
        when(stockAvailability.byStore(NEEDED.keySet())).thenReturn(Map.of(
                1, new StoreStock(q1, Map.of(10L, 2, 20L, 1)),
                2, new StoreStock(q5, Map.of(10L, 9, 20L, 9))));

        lifecycle.assignBranch(order, DeliveryType.HOME_DELIVERY, null, NEEDED, LABELS);

        assertThat(order.getStore()).isSameAs(q5);
        assertThat(order.getDeliveryType()).isEqualTo(DeliveryType.HOME_DELIVERY);
        assertThat(order.getShippingAddress()).isEqualTo("12 Nguyễn Trãi, Quận 5, TP. Hồ Chí Minh");

        // the nearest store lacks one unit → the farther store holding everything handles it
        when(stockAvailability.byStore(NEEDED.keySet())).thenReturn(Map.of(
                1, new StoreStock(q1, Map.of(10L, 2, 20L, 1)),
                2, new StoreStock(q5, Map.of(10L, 1, 20L, 9))));
        Order farther = new Order();
        farther.setShippingAddress("12 Nguyễn Trãi, Quận 5, TP. Hồ Chí Minh");
        lifecycle.assignBranch(farther, DeliveryType.HOME_DELIVERY, null, NEEDED, LABELS);
        assertThat(farther.getStore()).isSameAs(q1);
    }

    @Test
    void assignBranch_homeDelivery_equallyNearStores_theOneWithMoreUnitsWins() {
        Store a = store(1, "POY A", true);
        Store b = store(2, "POY B", true);
        Order order = new Order();
        order.setShippingAddress("Phường 3, Quận Gò Vấp");
        when(stockAvailability.byStore(NEEDED.keySet())).thenReturn(Map.of(
                1, new StoreStock(a, Map.of(10L, 2, 20L, 1)),
                2, new StoreStock(b, Map.of(10L, 6, 20L, 4))));

        lifecycle.assignBranch(order, DeliveryType.HOME_DELIVERY, null, NEEDED, LABELS);

        assertThat(order.getStore()).isSameAs(b);
    }

    @Test
    void assignBranch_homeDelivery_noStoreHoldsTheWholeOrder_isOutOfStock() {
        Store a = store(1, "POY A", true);
        Store b = store(2, "POY B", true);
        Order order = new Order();
        order.setShippingAddress("Quận 1");
        // enough units in total, but split between the stores
        when(stockAvailability.byStore(NEEDED.keySet())).thenReturn(Map.of(
                1, new StoreStock(a, Map.of(10L, 2)),
                2, new StoreStock(b, Map.of(20L, 1))));
        assertThatThrownBy(() -> lifecycle.assignBranch(order, DeliveryType.HOME_DELIVERY, null, NEEDED, LABELS))
                .isInstanceOfSatisfying(BusinessException.class, ex -> {
                    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.OUT_OF_STOCK);
                    assertThat(ex.getMessage()).startsWith("Không chi nhánh nào có đủ cả đơn");
                });

        // not enough units at all (and no open store with any row)
        when(stockAvailability.byStore(NEEDED.keySet())).thenReturn(Map.of(1, new StoreStock(a, Map.of(10L, 1))));
        assertThatThrownBy(() -> lifecycle.assignBranch(order, DeliveryType.HOME_DELIVERY, null, NEEDED, LABELS))
                .hasMessage("Không đủ hàng: Điện thoại - Đen (cần 2, còn 1); Ốp lưng - Trong (cần 1, còn 0)");
        when(stockAvailability.byStore(NEEDED.keySet())).thenReturn(Map.of());
        assertThatThrownBy(() -> lifecycle.assignBranch(order, DeliveryType.HOME_DELIVERY, null, NEEDED, LABELS))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.OUT_OF_STOCK));
        assertThat(order.getStore()).isNull();
    }
    @Test
    void onOrderConfirmed_takesTheSummedQuantityOfEveryVariant_andWritesOneOutMovementEach() {
        Store store = store(3, "POY Quận 1", true);
        ProductVariant phone = variant(10L, "Đen 128GB");
        ProductVariant cover = variant(20L, "Ốp lưng");
        Order order = order(store);
        when(orderItemRepository.findAllWithProductByOrderIdIn(List.of(5L)))
                .thenReturn(List.of(item(phone, 2), item(cover, 1), item(phone, 1)));
        Inventory phoneRow = new Inventory(store, phone, 5);
        Inventory coverRow = new Inventory(store, cover, 1);
        when(inventoryRepository.findAllForUpdate(3, List.of(10L, 20L))).thenReturn(List.of(phoneRow, coverRow));
        User staff = new User();

        lifecycle.onOrderConfirmed(order, staff);

        assertThat(phoneRow.getQuantity()).isEqualTo(2);
        assertThat(coverRow.getQuantity()).isZero();
        ArgumentCaptor<StockMovement> saved = ArgumentCaptor.forClass(StockMovement.class);
        verify(stockMovementRepository, times(2)).save(saved.capture());
        assertThat(saved.getAllValues()).extracting(m -> m.getVariant().getId(), StockMovement::getQuantityChange)
                .containsExactly(org.assertj.core.groups.Tuple.tuple(10L, -3), org.assertj.core.groups.Tuple.tuple(20L, -1));
        assertThat(saved.getAllValues()).allSatisfy(m -> {
            assertThat(m.getMovementType()).isEqualTo(MovementType.OUT);
            assertThat(m.getOrder()).isSameAs(order);
            assertThat(m.getStore()).isSameAs(store);
            assertThat(m.getCreatedBy()).isSameAs(staff);
        });
    }

    @Test
    void onOrderConfirmed_short_listsEveryMissingVariant_andChangesNothing() {
        Store store = store(3, "POY Quận 1", true);
        ProductVariant phone = variant(10L, "Đen 128GB");
        ProductVariant cover = variant(20L, "Ốp lưng");
        Order order = order(store);
        when(orderItemRepository.findAllWithProductByOrderIdIn(List.of(5L)))
                .thenReturn(List.of(item(phone, 3), item(cover, 1)));
        Inventory phoneRow = new Inventory(store, phone, 2);
        // the cover was never stocked at this store: no row, counts as 0
        when(inventoryRepository.findAllForUpdate(3, List.of(10L, 20L))).thenReturn(List.of(phoneRow));

        assertThatThrownBy(() -> lifecycle.onOrderConfirmed(order, new User()))
                .isInstanceOfSatisfying(BusinessException.class, ex -> {
                    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INSUFFICIENT_STOCK);
                    assertThat(ex.getMessage()).contains("POY Quận 1",
                            "Điện thoại Test - Đen 128GB (cần 3, còn 2)", "Điện thoại Test - Ốp lưng (cần 1, còn 0)");
                });
        assertThat(phoneRow.getQuantity()).isEqualTo(2);
        verify(stockMovementRepository, never()).save(any());
    }

    @Test
    void onOrderConfirmed_withoutAStore_isRefusedBeforeReadingAnything() {
        assertThatThrownBy(() -> lifecycle.onOrderConfirmed(order(null), new User()))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.ORDER_STORE_MISSING));
        verifyNoInteractions(orderItemRepository, inventoryRepository, stockMovementRepository);
    }

    @Test
    void onOrderCancelled_fromConfirmed_givesBackWhatTheConfirmationTook() {
        Store store = store(3, "POY Quận 1", true);
        ProductVariant phone = variant(10L, "Đen 128GB");
        ProductVariant cover = variant(20L, "Ốp lưng");
        Order order = order(store);
        when(stockMovementRepository.findByOrderIdAndMovementTypeOrderById(5L, MovementType.OUT))
                .thenReturn(List.of(out(store, cover, -1), out(store, phone, -3)));
        Inventory phoneRow = new Inventory(store, phone, 0);
        Inventory coverRow = new Inventory(store, cover, 4);
        when(inventoryRepository.findAllForUpdate(3, List.of(10L, 20L))).thenReturn(List.of(phoneRow, coverRow));
        User staff = new User();

        lifecycle.onOrderCancelled(order, OrderStatus.CONFIRMED, staff);

        assertThat(phoneRow.getQuantity()).isEqualTo(3);
        assertThat(coverRow.getQuantity()).isEqualTo(5);
        ArgumentCaptor<StockMovement> saved = ArgumentCaptor.forClass(StockMovement.class);
        verify(stockMovementRepository, times(2)).save(saved.capture());
        assertThat(saved.getAllValues()).extracting(m -> m.getVariant().getId(), StockMovement::getQuantityChange)
                .containsExactly(org.assertj.core.groups.Tuple.tuple(10L, 3), org.assertj.core.groups.Tuple.tuple(20L, 1));
        assertThat(saved.getAllValues()).allSatisfy(m -> {
            assertThat(m.getMovementType()).isEqualTo(MovementType.RETURN);
            assertThat(m.getOrder()).isSameAs(order);
            assertThat(m.getCreatedBy()).isSameAs(staff);
        });
    }

    @Test
    void onOrderCancelled_notFromConfirmed_orNothingTaken_isANoOp() {
        Order order = order(store(3, "POY Quận 1", true));

        lifecycle.onOrderCancelled(order, OrderStatus.PENDING, new User());
        verifyNoInteractions(stockMovementRepository, inventoryRepository);

        // confirmed before stock was tracked: no OUT movement, nothing to give back
        when(stockMovementRepository.findByOrderIdAndMovementTypeOrderById(5L, MovementType.OUT)).thenReturn(List.of());
        lifecycle.onOrderCancelled(order, OrderStatus.CONFIRMED, new User());
        verifyNoInteractions(inventoryRepository);
        verify(stockMovementRepository, never()).save(any());
    }

    @Test
    void reassignStore_movesThePickupAddress_andRefusesClosedOrUnknownStores() {
        Order pickup = order(store(3, "POY Quận 1", true));
        pickup.setDeliveryType(DeliveryType.PICKUP);
        Store q5 = store(4, "POY Quận 5", true);
        q5.setDistrict("Quận 5");
        when(storeRepository.findById(4)).thenReturn(Optional.of(q5));
        when(storeRepository.findById(6)).thenReturn(Optional.of(store(6, "Đóng", false)));
        when(storeRepository.findById(7)).thenReturn(Optional.empty());

        lifecycle.reassignStore(pickup, 4);

        assertThat(pickup.getStore()).isSameAs(q5);
        assertThat(pickup.getShippingAddress())
                .isEqualTo("Nhận tại cửa hàng: POY Quận 5, 12 Nguyễn Huệ, Quận 5, Hồ Chí Minh");

        Order home = order(store(3, "POY Quận 1", true));
        home.setShippingAddress("1 Lê Lợi, Quận 1");
        lifecycle.reassignStore(home, 4);
        assertThat(home.getStore()).isSameAs(q5);
        assertThat(home.getShippingAddress()).isEqualTo("1 Lê Lợi, Quận 1");

        assertThatThrownBy(() -> lifecycle.reassignStore(home, 6))
                .isInstanceOfSatisfying(BusinessException.class, ex -> assertThat(ex.getDetails()).containsKey("storeId"));
        assertThatThrownBy(() -> lifecycle.reassignStore(home, 7))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.STORE_NOT_FOUND));
        assertThat(home.getStore()).isSameAs(q5);
    }

    private static Order order(Store store) {
        Order order = new Order();
        order.setId(5L);
        order.setStore(store);
        return order;
    }

    private static ProductVariant variant(Long id, String name) {
        Product product = new Product();
        product.setId(id * 100);
        product.setName("Điện thoại Test");
        ProductVariant variant = new ProductVariant();
        variant.setId(id);
        variant.setVariantName(name);
        variant.setProduct(product);
        return variant;
    }

    private static OrderItem item(ProductVariant variant, int quantity) {
        OrderItem item = new OrderItem();
        item.setVariant(variant);
        item.setQuantity(quantity);
        return item;
    }

    private static StockMovement out(Store store, ProductVariant variant, int change) {
        StockMovement movement = new StockMovement();
        movement.setStore(store);
        movement.setVariant(variant);
        movement.setMovementType(MovementType.OUT);
        movement.setQuantityChange(change);
        return movement;
    }

    private void assertInvalid(DeliveryType type, Integer storeId, String address, String field) {
        assertThatThrownBy(() -> lifecycle.validateDeliverySelection(type, storeId, address))
                .isInstanceOfSatisfying(BusinessException.class, ex -> assertThat(ex.getDetails()).containsKey(field));
    }

    private static Store store(int id, String name, boolean active) {
        Store store = new Store();
        store.setId(id);
        store.setName(name);
        store.setAddress("12 Nguyễn Huệ");
        store.setDistrict("Quận 1");
        store.setCity("Hồ Chí Minh");
        store.setActive(active);
        return store;
    }
}
