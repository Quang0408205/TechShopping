package com.example.Tech.service.impl.product;

import com.example.Tech.dto.request.product.AttributeCreateRequest;
import com.example.Tech.dto.request.product.AttributeUpdateRequest;
import com.example.Tech.dto.response.common.PageResponse;
import com.example.Tech.dto.response.product.AttributeResponse;
import com.example.Tech.entity.product.Attribute;
import com.example.Tech.exception.BusinessException;
import com.example.Tech.exception.ErrorCode;
import com.example.Tech.exception.ResourceNotFoundException;
import com.example.Tech.mapper.product.AttributeMapper;
import com.example.Tech.repository.product.AttributeRepository;
import com.example.Tech.repository.product.AttributeValueRepository;
import com.example.Tech.service.product.AttributeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AttributeServiceImpl implements AttributeService {

    private final AttributeRepository attributeRepository;
    private final AttributeValueRepository attributeValueRepository;
    private final AttributeMapper attributeMapper;

    @Override
    public PageResponse<AttributeResponse> getAll(Pageable pageable) {
        return PageResponse.from(attributeRepository.findAll(pageable).map(attributeMapper::toResponse));
    }

    @Override
    public AttributeResponse getById(Integer id) {
        return attributeMapper.toResponse(findAttribute(id));
    }

    @Override
    @Transactional
    public AttributeResponse create(AttributeCreateRequest request) {
        String name = request.name().trim();
        if (attributeRepository.existsByName(name)) {
            throw duplicate(name);
        }

        Attribute attribute = attributeMapper.toEntity(request);
        attribute.setName(name);

        Attribute saved = attributeRepository.save(attribute);
        log.info("Created attribute id={}", saved.getId());
        return attributeMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public AttributeResponse update(Integer id, AttributeUpdateRequest request) {
        Attribute attribute = findAttribute(id);

        String name = request.name().trim();
        if (attributeRepository.existsByNameAndIdNot(name, id)) {
            throw duplicate(name);
        }

        attributeMapper.updateEntity(attribute, request);
        attribute.setName(name);

        Attribute saved = attributeRepository.saveAndFlush(attribute);
        log.info("Updated attribute id={}", id);
        return attributeMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void delete(Integer id) {
        Attribute attribute = findAttribute(id);
        if (attributeValueRepository.existsByAttributeId(id)) {
            throw new BusinessException(ErrorCode.RESOURCE_IN_USE,
                    "Attribute %d still has values".formatted(id));
        }
        attributeRepository.delete(attribute);
        attributeRepository.flush();
        log.info("Deleted attribute id={}", id);
    }

    private Attribute findAttribute(Integer id) {
        return attributeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.ATTRIBUTE_NOT_FOUND, id));
    }

    private BusinessException duplicate(String name) {
        return new BusinessException(ErrorCode.DUPLICATE_ATTRIBUTE,
                "Attribute with name '%s' already exists".formatted(name));
    }
}
