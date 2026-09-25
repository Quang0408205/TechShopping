package com.example.Tech.service.product;

import com.example.Tech.dto.request.product.AttributeValueCreateRequest;
import com.example.Tech.dto.request.product.AttributeValueUpdateRequest;
import com.example.Tech.dto.response.product.AttributeValueResponse;

import java.util.List;

public interface AttributeValueService {

    List<AttributeValueResponse> getByAttributeId(Integer attributeId);

    AttributeValueResponse getById(Integer id);

    AttributeValueResponse create(AttributeValueCreateRequest request);

    AttributeValueResponse update(Integer id, AttributeValueUpdateRequest request);

    void delete(Integer id);
}
