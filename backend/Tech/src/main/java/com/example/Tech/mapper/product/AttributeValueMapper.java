package com.example.Tech.mapper.product;

import com.example.Tech.dto.response.product.AttributeValueResponse;
import com.example.Tech.entity.product.AttributeValue;
import org.springframework.stereotype.Component;

/**
 * Entity -> response only; the value text and attribute are set by the service.
 */
@Component
public class AttributeValueMapper {

    public AttributeValueResponse toResponse(AttributeValue attributeValue) {
        return new AttributeValueResponse(
                attributeValue.getId(),
                attributeValue.getAttribute().getId(),
                attributeValue.getAttribute().getName(),
                attributeValue.getValue()
        );
    }
}
