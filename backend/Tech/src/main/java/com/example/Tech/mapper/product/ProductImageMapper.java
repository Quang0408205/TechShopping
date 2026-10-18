package com.example.Tech.mapper.product;

import com.example.Tech.dto.request.product.ProductImageCreateRequest;
import com.example.Tech.dto.request.product.ProductImageInput;
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
        image.setColor(cleanColor(request.color()));
        return image;
    }

    /** Image sent with a new product: its position in the list is its display order. */
    public ProductImage toEntity(ProductImageInput input, int displayOrder) {
        ProductImage image = new ProductImage();
        image.setImageUrl(input.imageUrl().trim());
        image.setAltText(input.altText());
        image.setDisplayOrder(displayOrder);
        image.setColor(cleanColor(input.color()));
        image.setPrimary(Boolean.TRUE.equals(input.isPrimary()));
        return image;
    }

    public void updateEntity(ProductImage image, ProductImageUpdateRequest request) {
        image.setImageUrl(request.imageUrl().trim());
        image.setAltText(request.altText());
        image.setDisplayOrder(request.displayOrder());
        image.setColor(cleanColor(request.color()));
    }

    private static String cleanColor(String color) {
        return color == null || color.isBlank() ? null : color.trim();
    }

    public ProductImageResponse toResponse(ProductImage image) {
        return new ProductImageResponse(
                image.getId(),
                image.getProduct().getId(),
                image.getImageUrl(),
                image.getAltText(),
                image.getDisplayOrder(),
                image.getPrimary(),
                image.getColor(),
                image.getUploadedAt()
        );
    }
}
