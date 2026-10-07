package com.example.Tech.service.store;

import com.example.Tech.dto.request.store.StoreRequest;
import com.example.Tech.dto.request.store.StoreSearchRequest;
import com.example.Tech.dto.response.common.PageResponse;
import com.example.Tech.dto.response.store.PublicStoreResponse;
import com.example.Tech.dto.response.store.StoreResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Branches (chi nhánh). Every admin method re-checks that the caller is still an ADMIN in the database.
 * A store is closed with active = false (it then stops being offered for pickup and receiving new orders);
 * hard delete is only allowed while nothing references it (STORE_IN_USE otherwise).
 */
public interface StoreService {

    PageResponse<StoreResponse> search(Long adminId, StoreSearchRequest filter, Pageable pageable);

    StoreResponse getById(Long adminId, Integer storeId);

    StoreResponse create(Long adminId, StoreRequest request);

    StoreResponse update(Long adminId, Integer storeId, StoreRequest request);

    void delete(Long adminId, Integer storeId);

    /** Open stores for customers, by name. */
    List<PublicStoreResponse> listOpen();
}
