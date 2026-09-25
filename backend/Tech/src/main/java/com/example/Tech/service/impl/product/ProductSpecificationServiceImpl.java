package com.example.Tech.service.impl.product;

import com.example.Tech.dto.request.product.ProductSpecificationCreateRequest;
import com.example.Tech.dto.request.product.ProductSpecificationUpdateRequest;
import com.example.Tech.dto.response.product.ProductSpecificationResponse;
import com.example.Tech.entity.product.Product;
import com.example.Tech.entity.product.ProductSpecification;
import com.example.Tech.exception.ErrorCode;
import com.example.Tech.exception.ResourceNotFoundException;
import com.example.Tech.mapper.product.ProductSpecificationMapper;
import com.example.Tech.repository.product.ProductRepository;
import com.example.Tech.repository.product.ProductSpecificationRepository;
import com.example.Tech.service.product.ProductSpecificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductSpecificationServiceImpl implements ProductSpecificationService {

    private final ProductSpecificationRepository specificationRepository;
    private final ProductRepository productRepository;
    private final ProductSpecificationMapper specificationMapper;

    @Override
    public List<ProductSpecificationResponse> getByProductId(Long productId) {
        Product product = findProduct(productId);
        return specificationRepository.findAllByProductIdOrderBySpecOrderAscIdAsc(product.getId()).stream()
                .map(specificationMapper::toResponse)
                .toList();
    }

    @Override
    public ProductSpecificationResponse getById(Long id) {
        return specificationMapper.toResponse(findSpecification(id));
    }

    @Override
    @Transactional
    public ProductSpecificationResponse create(ProductSpecificationCreateRequest request) {
        Product product = findProduct(request.productId());

        ProductSpecification specification = specificationMapper.toEntity(request);
        specification.setProduct(product);

        ProductSpecification saved = specificationRepository.save(specification);
        log.info("Created product specification id={} for product id={}", saved.getId(), product.getId());
        return specificationMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public ProductSpecificationResponse update(Long id, ProductSpecificationUpdateRequest request) {
        ProductSpecification specification = findSpecification(id);
        specificationMapper.updateEntity(specification, request);

        ProductSpecification saved = specificationRepository.saveAndFlush(specification);
        log.info("Updated product specification id={}", id);
        return specificationMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        ProductSpecification specification = findSpecification(id);
        specificationRepository.delete(specification);
        log.info("Deleted product specification id={}", id);
    }

    private ProductSpecification findSpecification(Long id) {
        return specificationRepository.findByIdAndProductDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.PRODUCT_SPECIFICATION_NOT_FOUND, id));
    }

    private Product findProduct(Long productId) {
        return productRepository.findByIdAndDeletedAtIsNull(productId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.PRODUCT_NOT_FOUND, productId));
    }
}
