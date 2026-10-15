package com.example.Tech.service.contact;

import com.example.Tech.dto.request.contact.ContactRequestCreateRequest;
import com.example.Tech.dto.request.contact.ContactRequestSearchRequest;
import com.example.Tech.dto.request.contact.ContactRequestUpdateRequest;
import com.example.Tech.dto.response.common.PageResponse;
import com.example.Tech.dto.response.contact.ContactRequestCreatedResponse;
import com.example.Tech.dto.response.contact.ContactRequestResponse;
import org.springframework.data.domain.Pageable;

public interface ContactRequestService {

    /** Public send; userId null for a guest. Rate limited per client IP and per email. */
    ContactRequestCreatedResponse create(Long userId, String clientIp, ContactRequestCreateRequest request);

    /** STAFF / BRANCH_MANAGER with a store, or ADMIN. */
    PageResponse<ContactRequestResponse> search(Long staffId, ContactRequestSearchRequest filter, Pageable pageable);

    /** STAFF / BRANCH_MANAGER with a store; ADMIN gets 403 ADMIN_READ_ONLY. */
    ContactRequestResponse update(Long staffId, Long id, ContactRequestUpdateRequest request);
}
