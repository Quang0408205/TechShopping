package com.example.Tech.service.impl.store;

import com.example.Tech.dto.request.store.StoreRequest;
import com.example.Tech.dto.response.store.PublicStoreResponse;
import com.example.Tech.dto.response.store.StoreResponse;
import com.example.Tech.entity.store.Store;
import com.example.Tech.entity.user.RoleName;
import com.example.Tech.entity.user.User;
import com.example.Tech.exception.BusinessException;
import com.example.Tech.exception.ErrorCode;
import com.example.Tech.repository.employee.EmployeeAssignmentRepository;
import com.example.Tech.repository.inventory.InventoryRepository;
import com.example.Tech.repository.order.OrderRepository;
import com.example.Tech.repository.store.StoreRepository;
import com.example.Tech.service.user.CurrentUserLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StoreServiceImplTest {

    private static final Long ADMIN_ID = 1L;

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private EmployeeAssignmentRepository assignmentRepository;

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CurrentUserLoader currentUserLoader;

    @InjectMocks
    private StoreServiceImpl storeService;

    @BeforeEach
    void setUp() {
        lenient().when(currentUserLoader.loadWithAnyRole(ADMIN_ID, RoleName.ADMIN)).thenReturn(new User());
    }

    @Test
    void create_trimsFields_blankOptionalsBecomeNull_andIsOpenByDefault() {
        when(storeRepository.save(any(Store.class))).thenAnswer(invocation -> {
            Store store = invocation.getArgument(0);
            store.setId(7);
            return store;
        });

        StoreResponse response = storeService.create(ADMIN_ID,
                request("  POY Quận 1 ", "  12 Nguyễn Huệ ", " Quận 1 ", " Hồ Chí Minh ", "   ", "", null, null, null));

        assertThat(response.id()).isEqualTo(7);
        assertThat(response.name()).isEqualTo("POY Quận 1");
        assertThat(response.address()).isEqualTo("12 Nguyễn Huệ");
        assertThat(response.district()).isEqualTo("Quận 1");
        assertThat(response.city()).isEqualTo("Hồ Chí Minh");
        assertThat(response.phone()).isNull();
        assertThat(response.email()).isNull();
        assertThat(response.active()).isTrue();
    }

    @Test
    void create_onlyOneCoordinate_isRejectedWithTheMissingField() {
        StoreRequest onlyLatitude = request("A", "B", "Quận 1", "Hồ Chí Minh", null, null,
                new BigDecimal("10.77"), null, true);

        assertThatThrownBy(() -> storeService.create(ADMIN_ID, onlyLatitude))
                .isInstanceOfSatisfying(BusinessException.class, ex -> {
                    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR);
                    assertThat(ex.getDetails()).containsKey("longitude");
                });
        verify(storeRepository, never()).save(any());
    }

    @Test
    void create_notAnAdmin_isDenied() {
        when(currentUserLoader.loadWithAnyRole(2L, RoleName.ADMIN))
                .thenThrow(new BusinessException(ErrorCode.ACCESS_DENIED));

        assertThatThrownBy(() -> storeService.create(2L, request("A", "B", "Q1", "HCM", null, null, null, null, null)))
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.ACCESS_DENIED);
        verify(storeRepository, never()).save(any());
    }

    @Test
    void update_activeFalse_closesTheStore() {
        Store store = store(3, "Cũ", true);
        when(storeRepository.findById(3)).thenReturn(Optional.of(store));
        when(storeRepository.saveAndFlush(store)).thenReturn(store);

        StoreResponse response = storeService.update(ADMIN_ID, 3,
                request("Mới", "Địa chỉ", "Quận 3", "Hồ Chí Minh", "0901234567", "a@poy.vn", null, null, false));

        assertThat(response.name()).isEqualTo("Mới");
        assertThat(response.active()).isFalse();
        assertThat(store.getActive()).isFalse();
    }

    @Test
    void update_unknownStore_isNotFound() {
        when(storeRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> storeService.update(ADMIN_ID, 99, request("A", "B", "Q1", "HCM", null, null, null, null, null)))
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.STORE_NOT_FOUND);
        verify(storeRepository, never()).saveAndFlush(any());
    }

    @Test
    void delete_storeWithEmployees_isInUse() {
        when(storeRepository.findById(3)).thenReturn(Optional.of(store(3, "A", true)));
        when(assignmentRepository.existsByStoreId(3)).thenReturn(true);

        assertThatThrownBy(() -> storeService.delete(ADMIN_ID, 3))
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.STORE_IN_USE);
        verify(storeRepository, never()).delete(any(Store.class));
    }

    @Test
    void delete_storeWithInventoryOrOrders_isInUse() {
        when(storeRepository.findById(3)).thenReturn(Optional.of(store(3, "A", true)));
        when(inventoryRepository.existsByIdStoreId(3)).thenReturn(false);
        when(orderRepository.existsByStoreId(3)).thenReturn(true);

        assertThatThrownBy(() -> storeService.delete(ADMIN_ID, 3))
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.STORE_IN_USE);
        verify(storeRepository, never()).delete(any(Store.class));
    }

    @Test
    void delete_unusedStore_isDeleted() {
        Store store = store(3, "A", true);
        when(storeRepository.findById(3)).thenReturn(Optional.of(store));

        storeService.delete(ADMIN_ID, 3);

        verify(storeRepository).delete(store);
    }

    @Test
    void listOpen_mapsThePublicFieldsOnly() {
        Store store = store(4, "POY Thủ Đức", true);
        store.setDistrict("Thủ Đức");
        store.setCity("Hồ Chí Minh");
        when(storeRepository.findAllByActiveTrueOrderByName()).thenReturn(List.of(store));

        List<PublicStoreResponse> open = storeService.listOpen();

        assertThat(open).containsExactly(
                new PublicStoreResponse(4, "POY Thủ Đức", "Địa chỉ 4", "Thủ Đức", "Hồ Chí Minh", null));
    }

    private static StoreRequest request(String name, String address, String district, String city, String phone,
                                        String email, BigDecimal latitude, BigDecimal longitude, Boolean active) {
        return new StoreRequest(name, address, district, city, phone, email, latitude, longitude, active);
    }

    private static Store store(int id, String name, boolean active) {
        Store store = new Store();
        store.setId(id);
        store.setName(name);
        store.setAddress("Địa chỉ " + id);
        store.setActive(active);
        return store;
    }
}
