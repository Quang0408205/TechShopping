package com.example.Tech.service.product;

import com.example.Tech.dto.request.product.AttributeCreateRequest;
import com.example.Tech.dto.request.product.AttributeUpdateRequest;
import com.example.Tech.dto.response.common.PageResponse;
import com.example.Tech.dto.response.product.AttributeResponse;
import org.springframework.data.domain.Pageable;

public interface AttributeService {

    PageResponse<AttributeResponse> getAll(Pageable pageable);

    AttributeResponse getById(Integer id);

    AttributeResponse create(AttributeCreateRequest request);

    AttributeResponse update(Integer id, AttributeUpdateRequest request);

    void delete(Integer id);
}
