package com.example.Tech.mapper.product;

import com.example.Tech.dto.request.product.ProductImageCreateRequest;
import com.example.Tech.dto.request.product.ProductImageUpdateRequest;
import com.example.Tech.dto.response.product.ProductImageResponse;
import com.example.Tech.entity.product.ProductImage;
import org.springframework.stereotype.Component;

/**
 * Maps simple fields only; product and the primary flag are resolved by the service.
 */
@Component
public class ProductImageMapper {

    public ProductImage toEntity(ProductImageCreateRequest request) {
        ProductImage image = new ProductImage();
        image.setImageUrl(request.imageUrl().trim());
        image.setAltText(request.altText());
        image.setDisplayOrder(request.displayOrder());
        return image;
    }

    public void updateEntity(ProductImage image, ProductImageUpdateRequest request) {
        image.setImageUrl(request.imageUrl().trim());
        image.setAltText(request.altText());
        image.setDisplayOrder(request.displayOrder());
    }

    public ProductImageResponse toResponse(ProductImage image) {
        return new ProductImageResponse(
                image.getId(),
                image.getProduct().getId(),
                image.getImageUrl(),
                image.getAltText(),
                image.getDisplayOrder(),
                image.getPrimary(),
                image.getUploadedAt()
        );
    }
}
