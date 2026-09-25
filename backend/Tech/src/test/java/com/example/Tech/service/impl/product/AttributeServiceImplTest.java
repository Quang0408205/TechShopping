package com.example.Tech.service.impl.product;

import com.example.Tech.dto.request.product.AttributeCreateRequest;
import com.example.Tech.dto.request.product.AttributeUpdateRequest;
import com.example.Tech.dto.response.product.AttributeResponse;
import com.example.Tech.entity.product.Attribute;
import com.example.Tech.exception.BusinessException;
import com.example.Tech.exception.ErrorCode;
import com.example.Tech.exception.ResourceNotFoundException;
import com.example.Tech.mapper.product.AttributeMapper;
import com.example.Tech.repository.product.AttributeRepository;
import com.example.Tech.repository.product.AttributeValueRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttributeServiceImplTest {

    @Mock
    private AttributeRepository attributeRepository;

    @Mock
    private AttributeValueRepository attributeValueRepository;

    private AttributeServiceImpl attributeService;

    @BeforeEach
    void setUp() {
        attributeService = new AttributeServiceImpl(attributeRepository, attributeValueRepository,
                new AttributeMapper());
    }

    @Test
    void create_success_trimsName() {
        when(attributeRepository.existsByName("Màu sắc")).thenReturn(false);
        when(attributeRepository.save(any(Attribute.class))).thenAnswer(invocation -> {
            Attribute attribute = invocation.getArgument(0);
            attribute.setId(1);
            return attribute;
        });

        AttributeResponse response = attributeService.create(new AttributeCreateRequest("  Màu sắc ", null, "color"));

        assertThat(response.id()).isEqualTo(1);
        assertThat(response.name()).isEqualTo("Màu sắc");
        assertThat(response.attributeType()).isEqualTo("color");
    }

    @Test
    void create_duplicateName_throwsConflict() {
        when(attributeRepository.existsByName("RAM")).thenReturn(true);

        assertThatThrownBy(() -> attributeService.create(new AttributeCreateRequest("RAM", null, null)))
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.DUPLICATE_ATTRIBUTE);
        verify(attributeRepository, never()).save(any());
    }

    @Test
    void getById_notFound_throws() {
        when(attributeRepository.findById(1)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> attributeService.getById(1))
                .isInstanceOf(ResourceNotFoundException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.ATTRIBUTE_NOT_FOUND);
    }

    @Test
    void update_success() {
        Attribute existing = attribute(1, "RAM");
        when(attributeRepository.findById(1)).thenReturn(Optional.of(existing));
        when(attributeRepository.existsByNameAndIdNot("Dung lượng RAM", 1)).thenReturn(false);
        when(attributeRepository.saveAndFlush(existing)).thenReturn(existing);

        AttributeResponse response = attributeService.update(1,
                new AttributeUpdateRequest("Dung lượng RAM", "desc", "memory"));

        assertThat(response.name()).isEqualTo("Dung lượng RAM");
        assertThat(response.description()).isEqualTo("desc");
    }

    @Test
    void update_duplicateName_throwsConflict() {
        when(attributeRepository.findById(1)).thenReturn(Optional.of(attribute(1, "RAM")));
        when(attributeRepository.existsByNameAndIdNot("Màu sắc", 1)).thenReturn(true);

        assertThatThrownBy(() -> attributeService.update(1, new AttributeUpdateRequest("Màu sắc", null, null)))
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.DUPLICATE_ATTRIBUTE);
    }

    @Test
    void delete_success() {
        Attribute existing = attribute(1, "RAM");
        when(attributeRepository.findById(1)).thenReturn(Optional.of(existing));
        when(attributeValueRepository.existsByAttributeId(1)).thenReturn(false);

        attributeService.delete(1);

        verify(attributeRepository).delete(existing);
    }

    @Test
    void delete_withValues_throwsResourceInUse() {
        when(attributeRepository.findById(1)).thenReturn(Optional.of(attribute(1, "RAM")));
        when(attributeValueRepository.existsByAttributeId(1)).thenReturn(true);

        assertThatThrownBy(() -> attributeService.delete(1))
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.RESOURCE_IN_USE);
        verify(attributeRepository, never()).delete(any());
    }

    private static Attribute attribute(Integer id, String name) {
        Attribute attribute = new Attribute();
        attribute.setId(id);
        attribute.setName(name);
        return attribute;
    }
}
