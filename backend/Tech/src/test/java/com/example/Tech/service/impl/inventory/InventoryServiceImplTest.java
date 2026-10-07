package com.example.Tech.service.impl.inventory;

import com.example.Tech.dto.request.inventory.InventorySearchRequest;
import com.example.Tech.dto.request.inventory.StockInRequest;
import com.example.Tech.dto.request.inventory.StockInStatsRequest;
import com.example.Tech.dto.response.common.PageResponse;
import com.example.Tech.dto.response.inventory.InventoryItemResponse;
import com.example.Tech.dto.response.inventory.InventoryOverviewResponse;
import com.example.Tech.dto.response.inventory.StockInStatsResponse;
import com.example.Tech.entity.inventory.Inventory;
import com.example.Tech.entity.inventory.MovementType;
import com.example.Tech.entity.inventory.StockMovement;
import com.example.Tech.entity.product.Product;
import com.example.Tech.entity.product.ProductVariant;
import com.example.Tech.entity.store.Store;
import com.example.Tech.entity.user.RoleName;
import com.example.Tech.entity.user.User;
import com.example.Tech.exception.BusinessException;
import com.example.Tech.exception.ErrorCode;
import com.example.Tech.repository.inventory.InventoryRepository;
import com.example.Tech.repository.inventory.StockInStoreTotals;
import com.example.Tech.repository.inventory.StockInTotals;
import com.example.Tech.repository.inventory.StockMovementRepository;
import com.example.Tech.repository.product.ProductImageRepository;
import com.example.Tech.repository.product.ProductVariantRepository;
import com.example.Tech.repository.store.StoreRepository;
import com.example.Tech.service.store.StoreAccessGuard;
import com.example.Tech.service.user.CurrentUserLoader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryServiceImplTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private StockMovementRepository stockMovementRepository;

    @Mock
    private ProductVariantRepository variantRepository;

    @Mock
    private ProductImageRepository imageRepository;

    @Mock
    private StoreAccessGuard storeAccessGuard;

    @Mock
    private CurrentUserLoader currentUserLoader;

    @Mock
    private StoreRepository storeRepository;

    @InjectMocks
    private InventoryServiceImpl service;

    @Test
    void stockIn_addsToTheLockedRow_andRecordsAnInMovementByTheCaller() {
        Store store = new Store();
        store.setId(3);
        User staff = new User();
        staff.setId(8L);
        ProductVariant variant = variant(120L);
        Inventory row = new Inventory(store, variant, 4);
        when(storeAccessGuard.require(8L, 3)).thenReturn(new StoreAccessGuard.Access(staff, store));
        when(variantRepository.findByIdAndProductDeletedAtIsNull(120L)).thenReturn(Optional.of(variant));
        when(inventoryRepository.findAllForUpdate(3, List.of(120L))).thenReturn(List.of(row));

        InventoryItemResponse response = service.stockIn(8L, 3,
                new StockInRequest(120L, 6, "  Công ty ABC ", "   "));

        assertThat(response.quantity()).isEqualTo(10);
        assertThat(row.getQuantity()).isEqualTo(10);
        verify(inventoryRepository).insertIfMissing(3, 120L);
        ArgumentCaptor<StockMovement> movement = ArgumentCaptor.forClass(StockMovement.class);
        verify(stockMovementRepository).save(movement.capture());
        assertThat(movement.getValue().getMovementType()).isEqualTo(MovementType.IN);
        assertThat(movement.getValue().getQuantityChange()).isEqualTo(6);
        assertThat(movement.getValue().getSupplierName()).isEqualTo("Công ty ABC");
        assertThat(movement.getValue().getNote()).isNull();
        assertThat(movement.getValue().getCreatedBy()).isSameAs(staff);
        assertThat(movement.getValue().getStore()).isSameAs(store);
    }

    @Test
    void stockIn_unknownOrDeletedVariant_isNotFound_andNothingIsWritten() {
        when(storeAccessGuard.require(8L, 3)).thenReturn(new StoreAccessGuard.Access(new User(), new Store()));
        when(variantRepository.findByIdAndProductDeletedAtIsNull(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.stockIn(8L, 3, new StockInRequest(999L, 1, null, null)))
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.PRODUCT_VARIANT_NOT_FOUND);
        verify(inventoryRepository, never()).insertIfMissing(anyInt(), anyLong());
        verify(stockMovementRepository, never()).save(any());
    }

    @Test
    void stockIn_atAStoreTheCallerMayNotManage_writesNothing() {
        when(storeAccessGuard.require(8L, 4)).thenThrow(new BusinessException(ErrorCode.ACCESS_DENIED));

        assertThatThrownBy(() -> service.stockIn(8L, 4, new StockInRequest(120L, 1, null, null)))
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.ACCESS_DENIED);
        verify(inventoryRepository, never()).insertIfMissing(anyInt(), anyLong());
        verify(stockMovementRepository, never()).save(any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void overview_groupsTheStoreRowsOfEachVariant_byStoreName_withTheirTotal() {
        ProductVariant phone = variant(120L);
        ProductVariant tablet = variant(121L);
        Store b = store(2, "CN B", true);
        Store a = store(5, "CN A", false);
        Pageable pageable = PageRequest.of(0, 20);
        when(variantRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(phone, tablet), pageable, 2));
        when(inventoryRepository.findAllByIdVariantIdIn(List.of(120L, 121L))).thenReturn(List.of(
                new Inventory(b, phone, 3), new Inventory(a, phone, 4), new Inventory(b, tablet, 0)));

        PageResponse<InventoryOverviewResponse> page =
                service.overview(1L, new InventorySearchRequest(null, null), pageable);

        verify(currentUserLoader).loadWithAnyRole(1L, RoleName.ADMIN);
        InventoryOverviewResponse first = page.content().getFirst();
        assertThat(first.variantId()).isEqualTo(120L);
        assertThat(first.totalQuantity()).isEqualTo(7);
        assertThat(first.stores()).extracting(InventoryOverviewResponse.StoreStock::storeName)
                .containsExactly("CN A", "CN B");
        assertThat(first.stores().getFirst().storeActive()).isFalse();
        assertThat(first.stores().getFirst().quantity()).isEqualTo(4);
        assertThat(page.content().get(1).totalQuantity()).isZero();
        assertThat(page.content().get(1).stores()).hasSize(1);
    }

    @Test
    void overview_forANonAdmin_readsNothing() {
        when(currentUserLoader.loadWithAnyRole(8L, RoleName.ADMIN))
                .thenThrow(new BusinessException(ErrorCode.ACCESS_DENIED));

        assertThatThrownBy(() -> service.overview(8L, new InventorySearchRequest(null, null), PageRequest.of(0, 20)))
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.ACCESS_DENIED);
        verify(inventoryRepository, never()).findAllByIdVariantIdIn(anyCollection());
    }

    @Test
    void stockInStats_mergesStoreTotals_keepsOpenStoresWithoutStockIns_andSortsByQuantity() {
        Store open = store(1, "CN A", true);
        Store busy = store(2, "CN B", true);
        Store closedIdle = store(3, "CN C", false);
        Store closedBusy = store(4, "CN D", false);
        LocalDateTime from = LocalDateTime.of(2026, 10, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 10, 8, 0, 0);
        LocalDateTime last = LocalDateTime.of(2026, 10, 5, 9, 30);
        when(storeRepository.findAll(Sort.by("name", "id"))).thenReturn(List.of(open, busy, closedIdle, closedBusy));
        when(stockMovementRepository.stockInTotals(MovementType.IN, null, from, to))
                .thenReturn(new StockInTotals(14L, 3L, 2L, 1L));
        when(stockMovementRepository.stockInTotalsByStore(MovementType.IN, null, from, to)).thenReturn(List.of(
                new StockInStoreTotals(4, 4L, 1L, 1L, last), new StockInStoreTotals(2, 10L, 2L, 2L, last)));

        StockInStatsResponse stats = service.stockInStats(1L,
                new StockInStatsRequest(null, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 7)));

        verify(currentUserLoader).loadWithAnyRole(1L, RoleName.ADMIN);
        assertThat(stats.totalQuantity()).isEqualTo(14);
        assertThat(stats.stockInCount()).isEqualTo(3);
        assertThat(stats.supplierCount()).isEqualTo(1);
        assertThat(stats.stores()).extracting(StockInStatsResponse.StoreStockIn::storeName)
                .containsExactly("CN B", "CN D", "CN A");
        assertThat(stats.stores().get(1).storeActive()).isFalse();
        assertThat(stats.stores().get(2).quantity()).isZero();
        assertThat(stats.stores().get(2).lastStockInAt()).isNull();
    }

    @Test
    void stockInStats_fromAfterTo_orUnknownStore_isRefusedBeforeAnyAggregate() {
        assertThatThrownBy(() -> service.stockInStats(1L,
                new StockInStatsRequest(null, LocalDate.of(2026, 10, 8), LocalDate.of(2026, 10, 7))))
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
        when(storeRepository.findById(99)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.stockInStats(1L, new StockInStatsRequest(99, null, null)))
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.STORE_NOT_FOUND);
        verify(stockMovementRepository, never()).stockInTotals(any(), any(), any(), any());
    }

    private static Store store(int id, String name, boolean active) {
        Store store = new Store();
        store.setId(id);
        store.setName(name);
        store.setActive(active);
        return store;
    }

    private static ProductVariant variant(Long id) {
        Product product = new Product();
        product.setId(7L);
        product.setName("iPhone Test");
        ProductVariant variant = new ProductVariant();
        variant.setId(id);
        variant.setProduct(product);
        variant.setVariantName("Đen 128GB");
        return variant;
    }
}
