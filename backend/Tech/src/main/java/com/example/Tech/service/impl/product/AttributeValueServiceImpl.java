package com.example.Tech.service.impl.product;

import com.example.Tech.dto.request.product.AttributeValueCreateRequest;
import com.example.Tech.dto.request.product.AttributeValueUpdateRequest;
import com.example.Tech.dto.response.product.AttributeValueResponse;
import com.example.Tech.entity.product.Attribute;
import com.example.Tech.entity.product.AttributeValue;
import com.example.Tech.exception.BusinessException;
import com.example.Tech.exception.ErrorCode;
import com.example.Tech.exception.ResourceNotFoundException;
import com.example.Tech.mapper.product.AttributeValueMapper;
import com.example.Tech.repository.product.AttributeRepository;
import com.example.Tech.repository.product.AttributeValueRepository;
import com.example.Tech.service.product.AttributeValueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AttributeValueServiceImpl implements AttributeValueService {

    private final AttributeValueRepository attributeValueRepository;
    private final AttributeRepository attributeRepository;
    private final AttributeValueMapper attributeValueMapper;

    @Override
    public List<AttributeValueResponse> getByAttributeId(Integer attributeId) {
        findAttribute(attributeId);
        return attributeValueRepository.findAllByAttributeIdOrderByValueAsc(attributeId).stream()
                .map(attributeValueMapper::toResponse)
                .toList();
    }

    @Override
    public AttributeValueResponse getById(Integer id) {
        return attributeValueMapper.toResponse(findAttributeValue(id));
    }

    @Override
    @Transactional
    public AttributeValueResponse create(AttributeValueCreateRequest request) {
        Attribute attribute = findAttribute(request.attributeId());
        String value = request.value().trim();
        if (attributeValueRepository.existsByAttributeIdAndValue(attribute.getId(), value)) {
            throw duplicate(attribute, value);
        }

        AttributeValue attributeValue = new AttributeValue();
        attributeValue.setAttribute(attribute);
        attributeValue.setValue(value);

        AttributeValue saved = attributeValueRepository.save(attributeValue);
        log.info("Created attribute value id={} for attribute id={}", saved.getId(), attribute.getId());
        return attributeValueMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public AttributeValueResponse update(Integer id, AttributeValueUpdateRequest request) {
        AttributeValue attributeValue = findAttributeValue(id);
        Attribute attribute = attributeValue.getAttribute();
        String value = request.value().trim();
        if (attributeValueRepository.existsByAttributeIdAndValueAndIdNot(attribute.getId(), value, id)) {
            throw duplicate(attribute, value);
        }

        attributeValue.setValue(value);

        AttributeValue saved = attributeValueRepository.saveAndFlush(attributeValue);
        log.info("Updated attribute value id={}", id);
        return attributeValueMapper.toResponse(saved);
    }

    /**
     * Links to product variants are removed by the database (ON DELETE CASCADE).
     */
    @Override
    @Transactional
    public void delete(Integer id) {
        AttributeValue attributeValue = findAttributeValue(id);
        attributeValueRepository.delete(attributeValue);
        attributeValueRepository.flush();
        log.info("Deleted attribute value id={}", id);
    }

    private AttributeValue findAttributeValue(Integer id) {
        return attributeValueRepository.findWithAttributeById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.ATTRIBUTE_VALUE_NOT_FOUND, id));
    }

    private Attribute findAttribute(Integer attributeId) {
        return attributeRepository.findById(attributeId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.ATTRIBUTE_NOT_FOUND, attributeId));
    }

    private BusinessException duplicate(Attribute attribute, String value) {
        return new BusinessException(ErrorCode.DUPLICATE_ATTRIBUTE_VALUE,
                "Value '%s' already exists for attribute '%s'".formatted(value, attribute.getName()));
    }
}
