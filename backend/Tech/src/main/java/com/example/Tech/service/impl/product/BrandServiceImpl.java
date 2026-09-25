package com.example.Tech.service.impl.product;

import com.example.Tech.dto.request.product.BrandCreateRequest;
import com.example.Tech.dto.request.product.BrandUpdateRequest;
import com.example.Tech.dto.response.common.PageResponse;
import com.example.Tech.dto.response.product.BrandResponse;
import com.example.Tech.entity.product.Brand;
import com.example.Tech.exception.BusinessException;
import com.example.Tech.exception.ErrorCode;
import com.example.Tech.exception.ResourceNotFoundException;
import com.example.Tech.mapper.product.BrandMapper;
import com.example.Tech.repository.product.BrandRepository;
import com.example.Tech.repository.product.ProductRepository;
import com.example.Tech.service.product.BrandService;
import com.example.Tech.util.SlugUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BrandServiceImpl implements BrandService {

    private final BrandRepository brandRepository;
    private final ProductRepository productRepository;
    private final BrandMapper brandMapper;

    @Override
    public PageResponse<BrandResponse> getAll(Pageable pageable) {
        return PageResponse.from(brandRepository.findAll(pageable).map(brandMapper::toResponse));
    }

    @Override
    public BrandResponse getById(Integer id) {
        return brandMapper.toResponse(findBrand(id));
    }

    @Override
    @Transactional
    public BrandResponse create(BrandCreateRequest request) {
        String slug = resolveSlug(request.slug(), request.name());
        if (brandRepository.existsByName(request.name())) {
            throw duplicate("name", request.name());
        }
        if (brandRepository.existsBySlug(slug)) {
            throw duplicate("slug", slug);
        }

        Brand brand = brandMapper.toEntity(request);
        brand.setSlug(slug);

        Brand saved = brandRepository.save(brand);
        log.info("Created brand id={}", saved.getId());
        return brandMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public BrandResponse update(Integer id, BrandUpdateRequest request) {
        Brand brand = findBrand(id);

        String slug = resolveSlug(request.slug(), request.name());
        if (brandRepository.existsByNameAndIdNot(request.name(), id)) {
            throw duplicate("name", request.name());
        }
        if (brandRepository.existsBySlugAndIdNot(slug, id)) {
            throw duplicate("slug", slug);
        }

        brandMapper.updateEntity(brand, request);
        brand.setSlug(slug);

        Brand saved = brandRepository.saveAndFlush(brand);
        log.info("Updated brand id={}", id);
        return brandMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void delete(Integer id) {
        Brand brand = findBrand(id);
        if (productRepository.existsByBrandId(id)) {
            throw new BusinessException(ErrorCode.RESOURCE_IN_USE,
                    "Brand %d is still used by products".formatted(id));
        }
        brandRepository.delete(brand);
        brandRepository.flush();
        log.info("Deleted brand id={}", id);
    }

    private Brand findBrand(Integer id) {
        return brandRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.BRAND_NOT_FOUND, id));
    }

    private String resolveSlug(String slug, String name) {
        String resolved = SlugUtil.resolve(slug, name);
        if (resolved.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Cannot generate slug from brand name");
        }
        return resolved;
    }

    private BusinessException duplicate(String field, String value) {
        return new BusinessException(ErrorCode.DUPLICATE_BRAND,
                "Brand with %s '%s' already exists".formatted(field, value));
    }
}
