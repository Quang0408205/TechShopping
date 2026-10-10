package com.example.Tech.service.aftersales;

import com.example.Tech.dto.request.aftersales.MaintenanceRequestCreateRequest;
import com.example.Tech.dto.request.aftersales.ReturnRequestCreateRequest;
import com.example.Tech.dto.request.aftersales.ServiceRequestSearchRequest;
import com.example.Tech.dto.request.aftersales.WarrantyRequestCreateRequest;
import com.example.Tech.dto.response.aftersales.AfterSalesOrderResponse;
import com.example.Tech.dto.response.aftersales.ServiceRequestResponse;
import com.example.Tech.dto.response.common.PageResponse;
import com.example.Tech.entity.aftersales.ServiceRequestType;
import org.springframework.data.domain.Pageable;

/** The customer side of warranty / maintenance / return requests. */
public interface AfterSalesService {

    AfterSalesOrderResponse orderAfterSales(Long userId, Long orderId);

    ServiceRequestResponse createWarrantyRequest(Long userId, WarrantyRequestCreateRequest request);

    ServiceRequestResponse createMaintenanceRequest(Long userId, MaintenanceRequestCreateRequest request);

    ServiceRequestResponse createReturnRequest(Long userId, ReturnRequestCreateRequest request);

    PageResponse<ServiceRequestResponse> mine(Long userId, ServiceRequestSearchRequest filter, Pageable pageable);

    ServiceRequestResponse getMine(Long userId, ServiceRequestType type, Long id);

    ServiceRequestResponse cancel(Long userId, ServiceRequestType type, Long id);
}
