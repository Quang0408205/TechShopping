package com.example.Tech.mapper.product;

import com.example.Tech.dto.request.product.ProductSpecificationCreateRequest;
import com.example.Tech.dto.request.product.ProductSpecificationUpdateRequest;
import com.example.Tech.dto.response.product.ProductSpecificationResponse;
import com.example.Tech.entity.product.ProductSpecification;
import org.springframework.stereotype.Component;

/**
 * Maps simple fields only; the product is resolved by the service.
 */
@Component
public class ProductSpecificationMapper {

    public ProductSpecification toEntity(ProductSpecificationCreateRequest request) {
        ProductSpecification specification = new ProductSpecification();
        specification.setSpecName(request.specName().trim());
        specification.setSpecValue(request.specValue().trim());
        specification.setSpecOrder(request.specOrder());
        return specification;
    }

    public void updateEntity(ProductSpecification specification, ProductSpecificationUpdateRequest request) {
        specification.setSpecName(request.specName().trim());
        specification.setSpecValue(request.specValue().trim());
        specification.setSpecOrder(request.specOrder());
    }

    public ProductSpecificationResponse toResponse(ProductSpecification specification) {
        return new ProductSpecificationResponse(
                specification.getId(),
                specification.getProduct().getId(),
                specification.getSpecName(),
                specification.getSpecValue(),
                specification.getSpecOrder()
        );
    }
}
