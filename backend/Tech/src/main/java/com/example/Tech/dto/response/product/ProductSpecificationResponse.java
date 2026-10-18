package com.example.Tech.dto.response.product;

public record ProductSpecificationResponse(
        Long id,
        Long productId,
        String specName,
        String specValue,
        Integer specOrder,
        Long variantId
) {

    public ProductSpecificationResponse(Long id, Long productId, String specName, String specValue, Integer specOrder) {
        this(id, productId, specName, specValue, specOrder, null);
    }
}
