package com.example.Tech.mapper.product;

import com.example.Tech.dto.request.product.ProductCreateRequest;
import com.example.Tech.dto.request.product.ProductUpdateRequest;
import com.example.Tech.dto.response.product.ProductResponse;
import com.example.Tech.entity.product.Brand;
import com.example.Tech.entity.product.Category;
import com.example.Tech.entity.product.Product;
import org.springframework.stereotype.Component;

/**
 * Maps simple fields only; slug, sku, category and brand are resolved by the service.
 * Rating, review and view counters are never taken from requests.
 */
@Component
public class ProductMapper {

    public Product toEntity(ProductCreateRequest request) {
        Product product = new Product();
        product.setName(request.name());
        product.setDescription(request.description());
        product.setBasePrice(request.basePrice());
        product.setDiscountPrice(request.discountPrice());
        product.setWeight(request.weight());
        if (request.stockQuantity() != null) {
            product.setStockQuantity(request.stockQuantity());
        }
        if (request.warrantyMonths() != null) {
            product.setWarrantyMonths(request.warrantyMonths());
        }
        if (request.isActive() != null) {
            product.setActive(request.isActive());
        }
        return product;
    }

    public void updateEntity(Product product, ProductUpdateRequest request) {
        product.setName(request.name());
        product.setDescription(request.description());
        product.setBasePrice(request.basePrice());
        product.setDiscountPrice(request.discountPrice());
        product.setWeight(request.weight());
        if (request.stockQuantity() != null) {
            product.setStockQuantity(request.stockQuantity());
        }
        if (request.warrantyMonths() != null) {
            product.setWarrantyMonths(request.warrantyMonths());
        }
        if (request.isActive() != null) {
            product.setActive(request.isActive());
        }
    }

    public ProductResponse toResponse(Product product) {
        Category category = product.getCategory();
        Brand brand = product.getBrand();
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getSlug(),
                product.getDescription(),
                category.getId(),
                category.getName(),
                brand != null ? brand.getId() : null,
                brand != null ? brand.getName() : null,
                product.getBasePrice(),
                product.getDiscountPrice(),
                product.getStockQuantity(),
                product.getSku(),
                product.getWeight(),
                product.getWarrantyMonths(),
                product.getRating(),
                product.getTotalReviews(),
                product.getViewCount(),
                product.getActive(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }
}
