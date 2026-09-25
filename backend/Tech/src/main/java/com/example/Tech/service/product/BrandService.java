package com.example.Tech.service.product;

import com.example.Tech.dto.request.product.BrandCreateRequest;
import com.example.Tech.dto.request.product.BrandUpdateRequest;
import com.example.Tech.dto.response.common.PageResponse;
import com.example.Tech.dto.response.product.BrandResponse;
import org.springframework.data.domain.Pageable;

public interface BrandService {

    PageResponse<BrandResponse> getAll(Pageable pageable);

    BrandResponse getById(Integer id);

    BrandResponse create(BrandCreateRequest request);

    BrandResponse update(Integer id, BrandUpdateRequest request);

    void delete(Integer id);
}
