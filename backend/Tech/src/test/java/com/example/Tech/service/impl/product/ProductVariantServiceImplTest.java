package com.example.Tech.service.impl.product;

import com.example.Tech.dto.request.product.ProductVariantCreateRequest;
import com.example.Tech.dto.request.product.ProductVariantUpdateRequest;
import com.example.Tech.dto.response.product.AttributeValueResponse;
import com.example.Tech.dto.response.product.ProductVariantResponse;
import com.example.Tech.entity.product.Attribute;
import com.example.Tech.entity.product.AttributeValue;
import com.example.Tech.entity.product.Product;
import com.example.Tech.entity.product.ProductVariant;
import com.example.Tech.entity.product.VariantAttributeValue;
import com.example.Tech.entity.product.VariantAttributeValueId;
import com.example.Tech.exception.BusinessException;
import com.example.Tech.exception.ErrorCode;
import com.example.Tech.exception.ResourceNotFoundException;
import com.example.Tech.mapper.product.AttributeValueMapper;
import com.example.Tech.mapper.product.ProductVariantMapper;
import com.example.Tech.repository.product.AttributeValueRepository;
import com.example.Tech.repository.product.ProductRepository;
import com.example.Tech.repository.product.ProductVariantRepository;
import com.example.Tech.repository.product.VariantAttributeValueRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductVariantServiceImplTest {

    @Mock
    private ProductVariantRepository variantRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private AttributeValueRepository attributeValueRepository;

    @Mock
    private VariantAttributeValueRepository variantAttributeValueRepository;

    private ProductVariantServiceImpl variantService;

    @BeforeEach
    void setUp() {
        variantService = new ProductVariantServiceImpl(variantRepository, productRepository,
                attributeValueRepository, variantAttributeValueRepository,
                new ProductVariantMapper(new AttributeValueMapper()));
    }

    @Test
    void getById_includesAttributeValuesSortedByAttribute() {
        ProductVariant variant = variant();
        AttributeValue ram = attributeValue(20, attribute(2, "RAM"), "12 GB");
        AttributeValue color = attributeValue(10, attribute(1, "Màu sắc"), "Đen");
        when(variantRepository.findByIdAndProductDeletedAtIsNull(5L)).thenReturn(Optional.of(variant));
        when(variantAttributeValueRepository.findAllByVariantIdIn(List.of(5L)))
                .thenReturn(List.of(new VariantAttributeValue(variant, ram), new VariantAttributeValue(variant, color)));

        ProductVariantResponse response = variantService.getById(5L);

        assertThat(response.attributeValues())
                .extracting(AttributeValueResponse::attributeName, AttributeValueResponse::value)
                .containsExactly(tuple("Màu sắc", "Đen"), tuple("RAM", "12 GB"));
    }

    @Test
    void addAttributeValue_success() {
        ProductVariant variant = variant();
        AttributeValue color = attributeValue(10, attribute(1, "Màu sắc"), "Đen");
        when(variantRepository.findByIdAndProductDeletedAtIsNull(5L)).thenReturn(Optional.of(variant));
        when(attributeValueRepository.findWithAttributeById(10)).thenReturn(Optional.of(color));
        when(variantAttributeValueRepository.existsById(new VariantAttributeValueId(5L, 10))).thenReturn(false);
        when(variantAttributeValueRepository.existsByVariantIdAndAttributeValueAttributeId(5L, 1)).thenReturn(false);
        when(variantAttributeValueRepository.findAllByVariantIdIn(List.of(5L)))
                .thenReturn(List.of(new VariantAttributeValue(variant, color)));

        ProductVariantResponse response = variantService.addAttributeValue(5L, 10);

        ArgumentCaptor<VariantAttributeValue> captor = ArgumentCaptor.forClass(VariantAttributeValue.class);
        verify(variantAttributeValueRepository).save(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(new VariantAttributeValueId(5L, 10));
        assertThat(response.attributeValues()).extracting(AttributeValueResponse::value).containsExactly("Đen");
    }

    @Test
    void addAttributeValue_valueNotFound_throws() {
        when(variantRepository.findByIdAndProductDeletedAtIsNull(5L)).thenReturn(Optional.of(variant()));
        when(attributeValueRepository.findWithAttributeById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> variantService.addAttributeValue(5L, 99))
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.ATTRIBUTE_VALUE_NOT_FOUND);
    }

    @Test
    void addAttributeValue_alreadyAssigned_throwsConflict() {
        when(variantRepository.findByIdAndProductDeletedAtIsNull(5L)).thenReturn(Optional.of(variant()));
        when(attributeValueRepository.findWithAttributeById(10))
                .thenReturn(Optional.of(attributeValue(10, attribute(1, "Màu sắc"), "Đen")));
        when(variantAttributeValueRepository.existsById(new VariantAttributeValueId(5L, 10))).thenReturn(true);

        assertThatThrownBy(() -> variantService.addAttributeValue(5L, 10))
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.DUPLICATE_VARIANT_ATTRIBUTE_VALUE);
        verify(variantAttributeValueRepository, never()).save(any());
    }

    @Test
    void addAttributeValue_secondValueOfSameAttribute_throwsConflict() {
        when(variantRepository.findByIdAndProductDeletedAtIsNull(5L)).thenReturn(Optional.of(variant()));
        when(attributeValueRepository.findWithAttributeById(11))
                .thenReturn(Optional.of(attributeValue(11, attribute(1, "Màu sắc"), "Trắng")));
        when(variantAttributeValueRepository.existsById(new VariantAttributeValueId(5L, 11))).thenReturn(false);
        when(variantAttributeValueRepository.existsByVariantIdAndAttributeValueAttributeId(5L, 1)).thenReturn(true);

        assertThatThrownBy(() -> variantService.addAttributeValue(5L, 11))
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.VARIANT_ATTRIBUTE_CONFLICT);
        verify(variantAttributeValueRepository, never()).save(any());
    }

    @Test
    void removeAttributeValue_success() {
        when(variantRepository.findByIdAndProductDeletedAtIsNull(5L)).thenReturn(Optional.of(variant()));
        when(variantAttributeValueRepository.existsById(new VariantAttributeValueId(5L, 10))).thenReturn(true);

        variantService.removeAttributeValue(5L, 10);

        verify(variantAttributeValueRepository).deleteById(new VariantAttributeValueId(5L, 10));
    }

    @Test
    void removeAttributeValue_notAssigned_throwsNotFound() {
        when(variantRepository.findByIdAndProductDeletedAtIsNull(5L)).thenReturn(Optional.of(variant()));
        when(variantAttributeValueRepository.existsById(new VariantAttributeValueId(5L, 10))).thenReturn(false);

        assertThatThrownBy(() -> variantService.removeAttributeValue(5L, 10))
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.VARIANT_ATTRIBUTE_VALUE_NOT_FOUND);
        verify(variantAttributeValueRepository, never()).deleteById(any());
    }

    private static Attribute attribute(Integer id, String name) {
        Attribute attribute = new Attribute();
        attribute.setId(id);
        attribute.setName(name);
        return attribute;
    }

    private static AttributeValue attributeValue(Integer id, Attribute attribute, String text) {
        AttributeValue value = new AttributeValue();
        value.setId(id);
        value.setAttribute(attribute);
        value.setValue(text);
        return value;
    }

    @Test
    void create_success() {
        ProductVariantCreateRequest request = new ProductVariantCreateRequest(1L, "12GB/256GB - Đen", "V-1",
                new BigDecimal("20000000"), new BigDecimal("19000000"), null, "Đen", "256 GB", "12 GB");
        when(variantRepository.existsBySkuVariant("V-1")).thenReturn(false);
        when(productRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(product()));
        when(variantRepository.save(any(ProductVariant.class))).thenAnswer(invocation -> {
            ProductVariant variant = invocation.getArgument(0);
            variant.setId(10L);
            return variant;
        });

        ProductVariantResponse response = variantService.create(request);

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.productId()).isEqualTo(1L);
        assertThat(response.productName()).isEqualTo("Galaxy S26");
        assertThat(response.stockQuantity()).isZero();
        assertThat(response.storage()).isEqualTo("256 GB");
    }

    @Test
    void create_productNotFoundOrDeleted_throws() {
        ProductVariantCreateRequest request = new ProductVariantCreateRequest(99L, "V", null,
                new BigDecimal("1"), null, null, null, null, null);
        when(productRepository.findByIdAndDeletedAtIsNull(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> variantService.create(request))
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.PRODUCT_NOT_FOUND);
        verify(variantRepository, never()).save(any());
    }

    @Test
    void create_duplicateSku_throwsConflict() {
        ProductVariantCreateRequest request = new ProductVariantCreateRequest(1L, "V", "V-1",
                new BigDecimal("1"), null, null, null, null, null);
        when(variantRepository.existsBySkuVariant("V-1")).thenReturn(true);

        assertThatThrownBy(() -> variantService.create(request))
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.DUPLICATE_PRODUCT_VARIANT);
    }

    @Test
    void create_discountAbovePrice_throwsInvalidData() {
        ProductVariantCreateRequest request = new ProductVariantCreateRequest(1L, "V", null,
                new BigDecimal("100"), new BigDecimal("101"), null, null, null, null);

        assertThatThrownBy(() -> variantService.create(request))
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_PRODUCT_VARIANT_DATA);
    }

    @Test
    void getByProductId_returnsVariants() {
        when(productRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(product()));
        when(variantRepository.findAllByProductIdOrderByIdAsc(1L)).thenReturn(List.of(variant(), variant()));

        assertThat(variantService.getByProductId(1L)).hasSize(2);
    }

    @Test
    void getByProductId_productNotFound_throws() {
        when(productRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> variantService.getByProductId(1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getById_notFound_throws() {
        when(variantRepository.findByIdAndProductDeletedAtIsNull(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> variantService.getById(5L))
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.PRODUCT_VARIANT_NOT_FOUND);
    }

    @Test
    void update_success_blankSkuStoredAsNull() {
        ProductVariant existing = variant();
        ProductVariantUpdateRequest request = new ProductVariantUpdateRequest("8GB/128GB", " ",
                new BigDecimal("500"), null, 7, "Xanh", "128 GB", "8 GB");
        when(variantRepository.findByIdAndProductDeletedAtIsNull(5L)).thenReturn(Optional.of(existing));
        when(variantRepository.saveAndFlush(existing)).thenReturn(existing);

        ProductVariantResponse response = variantService.update(5L, request);

        assertThat(response.skuVariant()).isNull();
        assertThat(response.stockQuantity()).isEqualTo(7);
        assertThat(response.color()).isEqualTo("Xanh");
    }

    @Test
    void update_duplicateSku_throwsConflict() {
        when(variantRepository.findByIdAndProductDeletedAtIsNull(5L)).thenReturn(Optional.of(variant()));
        when(variantRepository.existsBySkuVariantAndIdNot("V-2", 5L)).thenReturn(true);
        ProductVariantUpdateRequest request = new ProductVariantUpdateRequest("V", "V-2",
                new BigDecimal("1"), null, null, null, null, null);

        assertThatThrownBy(() -> variantService.update(5L, request))
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.DUPLICATE_PRODUCT_VARIANT);
    }

    @Test
    void delete_success() {
        ProductVariant existing = variant();
        when(variantRepository.findByIdAndProductDeletedAtIsNull(5L)).thenReturn(Optional.of(existing));

        variantService.delete(5L);

        verify(variantRepository).delete(existing);
    }

    private static Product product() {
        Product product = new Product();
        product.setId(1L);
        product.setName("Galaxy S26");
        return product;
    }

    private static ProductVariant variant() {
        ProductVariant variant = new ProductVariant();
        variant.setId(5L);
        variant.setProduct(product());
        variant.setVariantName("Default");
        variant.setPrice(new BigDecimal("1000"));
        return variant;
    }
}
