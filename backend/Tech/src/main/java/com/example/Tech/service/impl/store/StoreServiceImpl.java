package com.example.Tech.service.impl.store;

import com.example.Tech.dto.request.store.StoreRequest;
import com.example.Tech.dto.request.store.StoreSearchRequest;
import com.example.Tech.dto.response.common.PageResponse;
import com.example.Tech.dto.response.store.PublicStoreResponse;
import com.example.Tech.dto.response.store.StoreResponse;
import com.example.Tech.entity.store.Store;
import com.example.Tech.entity.user.RoleName;
import com.example.Tech.exception.BusinessException;
import com.example.Tech.exception.ErrorCode;
import com.example.Tech.repository.employee.EmployeeAssignmentRepository;
import com.example.Tech.repository.inventory.InventoryRepository;
import com.example.Tech.repository.order.OrderRepository;
import com.example.Tech.repository.store.StoreFilterSpecifications;
import com.example.Tech.repository.store.StoreRepository;
import com.example.Tech.service.store.StoreService;
import com.example.Tech.service.user.CurrentUserLoader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StoreServiceImpl implements StoreService {

    private final StoreRepository storeRepository;
    private final EmployeeAssignmentRepository assignmentRepository;
    private final InventoryRepository inventoryRepository;
    private final OrderRepository orderRepository;
    private final CurrentUserLoader currentUserLoader;

    @Override
    public PageResponse<StoreResponse> search(Long adminId, StoreSearchRequest filter, Pageable pageable) {
        ensureAdmin(adminId);
        return PageResponse.from(storeRepository.findAll(StoreFilterSpecifications.matching(filter), pageable)
                .map(StoreServiceImpl::toResponse));
    }

    @Override
    public StoreResponse getById(Long adminId, Integer storeId) {
        ensureAdmin(adminId);
        return toResponse(find(storeId));
    }

    @Override
    @Transactional
    public StoreResponse create(Long adminId, StoreRequest request) {
        ensureAdmin(adminId);
        validate(request);
        Store store = new Store();
        apply(store, request);
        Store saved = storeRepository.save(store);
        log.info("Admin id={} created store id={}", adminId, saved.getId());
        return toResponse(saved);
    }

    @Override
    @Transactional
    public StoreResponse update(Long adminId, Integer storeId, StoreRequest request) {
        ensureAdmin(adminId);
        Store store = find(storeId);
        validate(request);
        apply(store, request);
        Store saved = storeRepository.saveAndFlush(store);
        log.info("Admin id={} updated store id={} (active={})", adminId, storeId, saved.getActive());
        return toResponse(saved);
    }

    @Override
    @Transactional
    public void delete(Long adminId, Integer storeId) {
        ensureAdmin(adminId);
        Store store = find(storeId);
        if (assignmentRepository.existsByStoreId(storeId) || inventoryRepository.existsByIdStoreId(storeId)
                || orderRepository.existsByStoreId(storeId)) {
            throw new BusinessException(ErrorCode.STORE_IN_USE);
        }
        storeRepository.delete(store);
        log.info("Admin id={} deleted store id={}", adminId, storeId);
    }

    @Override
    public List<PublicStoreResponse> listOpen() {
        return storeRepository.findAllByActiveTrueOrderByName().stream()
                .map(store -> new PublicStoreResponse(store.getId(), store.getName(), store.getAddress(),
                        store.getDistrict(), store.getCity(), store.getPhone()))
                .toList();
    }

    private void ensureAdmin(Long adminId) {
        currentUserLoader.loadWithAnyRole(adminId, RoleName.ADMIN);
    }

    private Store find(Integer storeId) {
        return storeRepository.findById(storeId).orElseThrow(() -> new BusinessException(ErrorCode.STORE_NOT_FOUND,
                "Không tìm thấy chi nhánh id %d".formatted(storeId)));
    }

    /** Coordinates are optional but come as a pair. */
    private static void validate(StoreRequest request) {
        if ((request.latitude() == null) != (request.longitude() == null)) {
            throw BusinessException.invalidField(request.latitude() == null ? "latitude" : "longitude",
                    "Nhập cả vĩ độ và kinh độ, hoặc để trống cả hai");
        }
    }

    private static void apply(Store store, StoreRequest request) {
        store.setName(request.name().trim());
        store.setAddress(request.address().trim());
        store.setDistrict(request.district().trim());
        store.setCity(request.city().trim());
        store.setPhone(trimToNull(request.phone()));
        store.setEmail(trimToNull(request.email()));
        store.setLatitude(request.latitude());
        store.setLongitude(request.longitude());
        store.setActive(!Boolean.FALSE.equals(request.active()));
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static StoreResponse toResponse(Store store) {
        return new StoreResponse(
                store.getId(),
                store.getName(),
                store.getAddress(),
                store.getDistrict(),
                store.getCity(),
                store.getPhone(),
                store.getEmail(),
                store.getLatitude(),
                store.getLongitude(),
                !Boolean.FALSE.equals(store.getActive()),
                store.getCreatedAt(),
                store.getUpdatedAt());
    }
}
