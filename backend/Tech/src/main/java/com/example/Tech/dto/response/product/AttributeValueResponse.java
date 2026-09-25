package com.example.Tech.dto.response.product;

public record AttributeValueResponse(
        Integer id,
        Integer attributeId,
        String attributeName,
        String value
) {
}
