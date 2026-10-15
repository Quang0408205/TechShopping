package com.example.Tech.service.aftersales;

import com.example.Tech.dto.request.aftersales.AdminServiceRequestSearchRequest;
import com.example.Tech.dto.request.aftersales.ServiceRequestUpdateRequest;
import com.example.Tech.dto.response.aftersales.ServiceRequestResponse;
import com.example.Tech.dto.response.common.PageResponse;
import com.example.Tech.entity.aftersales.ServiceRequestType;
import org.springframework.data.domain.Pageable;

/** Staff side of after-sales requests: a STAFF member only reaches requests of their store's orders. */
public interface AdminServiceRequestService {

    PageResponse<ServiceRequestResponse> search(Long staffId, AdminServiceRequestSearchRequest filter, Pageable pageable);

    ServiceRequestResponse get(Long staffId, ServiceRequestType type, Long id);

    ServiceRequestResponse update(Long staffId, ServiceRequestType type, Long id, ServiceRequestUpdateRequest request);
}
