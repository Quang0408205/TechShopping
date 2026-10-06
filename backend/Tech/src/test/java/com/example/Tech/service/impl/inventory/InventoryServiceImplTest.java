package com.example.Tech.service.impl.inventory;

import com.example.Tech.dto.request.inventory.StockInRequest;
import com.example.Tech.dto.response.inventory.InventoryItemResponse;
import com.example.Tech.entity.inventory.Inventory;
import com.example.Tech.entity.inventory.MovementType;
import com.example.Tech.entity.inventory.StockMovement;
import com.example.Tech.entity.product.Product;
import com.example.Tech.entity.product.ProductVariant;
import com.example.Tech.entity.store.Store;
import com.example.Tech.entity.user.User;
import com.example.Tech.exception.BusinessException;
import com.example.Tech.exception.ErrorCode;
import com.example.Tech.repository.inventory.InventoryRepository;
import com.example.Tech.repository.inventory.StockMovementRepository;
import com.example.Tech.repository.product.ProductImageRepository;
import com.example.Tech.repository.product.ProductVariantRepository;
import com.example.Tech.service.store.StoreAccessGuard;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
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
