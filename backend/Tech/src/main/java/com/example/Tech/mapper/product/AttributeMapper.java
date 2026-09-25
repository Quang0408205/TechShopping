package com.example.Tech.mapper.product;

import com.example.Tech.dto.request.product.AttributeCreateRequest;
import com.example.Tech.dto.request.product.AttributeUpdateRequest;
import com.example.Tech.dto.response.product.AttributeResponse;
import com.example.Tech.entity.product.Attribute;
import org.springframework.stereotype.Component;

/**
 * Maps simple fields only; the name is normalized by the service.
 */
@Component
public class AttributeMapper {

    public Attribute toEntity(AttributeCreateRequest request) {
        Attribute attribute = new Attribute();
        attribute.setDescription(request.description());
        attribute.setAttributeType(request.attributeType());
        return attribute;
    }

    public void updateEntity(Attribute attribute, AttributeUpdateRequest request) {
        attribute.setDescription(request.description());
        attribute.setAttributeType(request.attributeType());
    }

    public AttributeResponse toResponse(Attribute attribute) {
        return new AttributeResponse(
                attribute.getId(),
                attribute.getName(),
                attribute.getDescription(),
                attribute.getAttributeType()
        );
    }
}
