package com.example.Tech.service.impl.product;

import com.example.Tech.dto.request.product.AttributeValueCreateRequest;
import com.example.Tech.dto.request.product.AttributeValueUpdateRequest;
import com.example.Tech.dto.response.product.AttributeValueResponse;
import com.example.Tech.entity.product.Attribute;
import com.example.Tech.entity.product.AttributeValue;
import com.example.Tech.exception.BusinessException;
import com.example.Tech.exception.ErrorCode;
import com.example.Tech.mapper.product.AttributeValueMapper;
import com.example.Tech.repository.product.AttributeRepository;
import com.example.Tech.repository.product.AttributeValueRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttributeValueServiceImplTest {

    @Mock
    private AttributeValueRepository attributeValueRepository;

    @Mock
    private AttributeRepository attributeRepository;

    private AttributeValueServiceImpl attributeValueService;

    @BeforeEach
    void setUp() {
        attributeValueService = new AttributeValueServiceImpl(attributeValueRepository, attributeRepository,
                new AttributeValueMapper());
    }

    @Test
    void create_success() {
        when(attributeRepository.findById(1)).thenReturn(Optional.of(attribute()));
        when(attributeValueRepository.existsByAttributeIdAndValue(1, "Đen")).thenReturn(false);
        when(attributeValueRepository.save(any(AttributeValue.class))).thenAnswer(invocation -> {
            AttributeValue value = invocation.getArgument(0);
            value.setId(10);
            return value;
        });

        AttributeValueResponse response = attributeValueService.create(new AttributeValueCreateRequest(1, " Đen "));

        assertThat(response.id()).isEqualTo(10);
        assertThat(response.attributeName()).isEqualTo("Màu sắc");
        assertThat(response.value()).isEqualTo("Đen");
    }

    @Test
    void create_attributeNotFound_throws() {
        when(attributeRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> attributeValueService.create(new AttributeValueCreateRequest(99, "Đen")))
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.ATTRIBUTE_NOT_FOUND);
    }

    @Test
    void create_duplicateValue_throwsConflict() {
        when(attributeRepository.findById(1)).thenReturn(Optional.of(attribute()));
        when(attributeValueRepository.existsByAttributeIdAndValue(1, "Đen")).thenReturn(true);

        assertThatThrownBy(() -> attributeValueService.create(new AttributeValueCreateRequest(1, "Đen")))
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.DUPLICATE_ATTRIBUTE_VALUE);
        verify(attributeValueRepository, never()).save(any());
    }

    @Test
    void getByAttributeId_returnsValues() {
        when(attributeRepository.findById(1)).thenReturn(Optional.of(attribute()));
        when(attributeValueRepository.findAllByAttributeIdOrderByValueAsc(1))
                .thenReturn(List.of(value(10, "Đen"), value(11, "Trắng")));

        assertThat(attributeValueService.getByAttributeId(1))
                .extracting(AttributeValueResponse::value)
                .containsExactly("Đen", "Trắng");
    }

    @Test
    void getById_notFound_throws() {
        when(attributeValueRepository.findWithAttributeById(10)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> attributeValueService.getById(10))
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.ATTRIBUTE_VALUE_NOT_FOUND);
    }

    @Test
    void update_success() {
        AttributeValue existing = value(10, "Đen");
        when(attributeValueRepository.findWithAttributeById(10)).thenReturn(Optional.of(existing));
        when(attributeValueRepository.existsByAttributeIdAndValueAndIdNot(1, "Đen nhám", 10)).thenReturn(false);
        when(attributeValueRepository.saveAndFlush(existing)).thenReturn(existing);

        AttributeValueResponse response = attributeValueService.update(10, new AttributeValueUpdateRequest("Đen nhám"));

        assertThat(response.value()).isEqualTo("Đen nhám");
    }

    @Test
    void update_duplicateValue_throwsConflict() {
        when(attributeValueRepository.findWithAttributeById(10)).thenReturn(Optional.of(value(10, "Đen")));
        when(attributeValueRepository.existsByAttributeIdAndValueAndIdNot(1, "Trắng", 10)).thenReturn(true);

        assertThatThrownBy(() -> attributeValueService.update(10, new AttributeValueUpdateRequest("Trắng")))
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.DUPLICATE_ATTRIBUTE_VALUE);
    }

    @Test
    void delete_success() {
        AttributeValue existing = value(10, "Đen");
        when(attributeValueRepository.findWithAttributeById(10)).thenReturn(Optional.of(existing));

        attributeValueService.delete(10);

        verify(attributeValueRepository).delete(existing);
    }

    private static Attribute attribute() {
        Attribute attribute = new Attribute();
        attribute.setId(1);
        attribute.setName("Màu sắc");
        return attribute;
    }

    private static AttributeValue value(Integer id, String text) {
        AttributeValue value = new AttributeValue();
        value.setId(id);
        value.setAttribute(attribute());
        value.setValue(text);
        return value;
    }
}
