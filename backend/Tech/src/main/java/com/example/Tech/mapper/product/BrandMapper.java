package com.example.Tech.mapper.product;

import com.example.Tech.dto.request.product.BrandCreateRequest;
import com.example.Tech.dto.request.product.BrandUpdateRequest;
import com.example.Tech.dto.response.product.BrandResponse;
import com.example.Tech.entity.product.Brand;
import org.springframework.stereotype.Component;

/**
 * Maps simple fields only; slug is resolved by the service.
 */
@Component
public class BrandMapper {

    public Brand toEntity(BrandCreateRequest request) {
        Brand brand = new Brand();
        brand.setName(request.name());
        brand.setLogoUrl(request.logoUrl());
        brand.setDescription(request.description());
        brand.setWebsiteUrl(request.websiteUrl());
        if (request.isActive() != null) {
            brand.setActive(request.isActive());
        }
        return brand;
    }

    public void updateEntity(Brand brand, BrandUpdateRequest request) {
        brand.setName(request.name());
        brand.setLogoUrl(request.logoUrl());
        brand.setDescription(request.description());
        brand.setWebsiteUrl(request.websiteUrl());
        if (request.isActive() != null) {
            brand.setActive(request.isActive());
        }
    }

    public BrandResponse toResponse(Brand brand) {
        return new BrandResponse(
                brand.getId(),
                brand.getName(),
                brand.getSlug(),
                brand.getLogoUrl(),
                brand.getDescription(),
                brand.getWebsiteUrl(),
                brand.getActive(),
                brand.getCreatedAt(),
                brand.getUpdatedAt()
        );
    }
}
