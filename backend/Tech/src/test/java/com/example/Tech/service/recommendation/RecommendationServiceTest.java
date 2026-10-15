package com.example.Tech.service.recommendation;

import com.example.Tech.dto.request.product.ProductSearchRequest;
import com.example.Tech.dto.request.recommendation.RecommendationRequest;
import com.example.Tech.dto.response.common.PageResponse;
import com.example.Tech.dto.response.product.ProductResponse;
import com.example.Tech.dto.response.recommendation.RecommendationResponse;
import com.example.Tech.entity.recommendation.Recommendation;
import com.example.Tech.entity.recommendation.RecommendationType;
import com.example.Tech.exception.BusinessException;
import com.example.Tech.repository.recommendation.RecommendationRepository;
import com.example.Tech.service.product.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceTest {

    @Mock
    private AiRecommendationClient aiClient;

    @Mock
    private ProductService productService;

    @Mock
    private RecommendationRepository recommendationRepository;

    private RecommendationService service;

    @BeforeEach
    void setUp() {
        service = new RecommendationService(aiClient, productService, recommendationRepository, Clock.systemDefaultZone());
    }

    @Test
    void aiAnswer_keepsTheAiOrder_dropsHiddenProducts_andLogsForTheUser() {
        when(aiClient.fetch(eq(RecommendationType.PERSONAL), eq(7L), any(), eq(8), anyList())).thenReturn(Optional.of(List.of(
                new AiRecommendationClient.Item(2L, new BigDecimal("0.91234"), "Tương tự A"),
                new AiRecommendationClient.Item(3L, new BigDecimal("0.5"), "Hidden"),
                new AiRecommendationClient.Item(1L, new BigDecimal("0.4"), "Tương tự B"))));
        List<ProductResponse> visible = List.of(product(2L), product(1L));
        when(productService.getActiveByIds(List.of(2L, 3L, 1L))).thenReturn(visible);

        RecommendationResponse response = service.recommend(7L, new RecommendationRequest(RecommendationType.PERSONAL, null, null));

        assertThat(response.source()).isEqualTo(RecommendationService.SOURCE_AI);
        assertThat(response.items()).extracting(i -> i.product().id()).containsExactly(2L, 1L);
        assertThat(response.items().get(0).score()).isEqualByComparingTo("0.912");
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Recommendation>> logged = ArgumentCaptor.forClass(List.class);
        verify(recommendationRepository).saveAll(logged.capture());
        assertThat(logged.getValue()).hasSize(2).allMatch(r -> r.getUserId().equals(7L));
    }

    @Test
    void guest_isNotLogged() {
        when(aiClient.fetch(any(), eq(null), any(), anyInt(), anyList()))
                .thenReturn(Optional.of(List.of(new AiRecommendationClient.Item(2L, BigDecimal.ONE, "Phổ biến"))));
        List<ProductResponse> visible = List.of(product(2L));
        when(productService.getActiveByIds(List.of(2L))).thenReturn(visible);

        service.recommend(null, new RecommendationRequest(RecommendationType.PERSONAL, null, 4));

        verify(recommendationRepository, never()).saveAll(anyIterable());
    }

    @Test
    void aiDown_upsellFallback_isSameCategoryAndStrictlyMoreExpensive() {
        when(aiClient.fetch(any(), any(), any(), anyInt(), anyList())).thenReturn(Optional.empty());
        ProductResponse anchor = mock(ProductResponse.class);
        when(anchor.categoryId()).thenReturn(4);
        when(anchor.effectivePrice()).thenReturn(new BigDecimal("19990000"));
        when(productService.getById(37L)).thenReturn(anchor);
        PageResponse<ProductResponse> page = PageResponse.from(new PageImpl<>(List.of(product(5L))));
        when(productService.search(any(), any(Pageable.class))).thenReturn(page);

        RecommendationResponse response = service.recommend(null, new RecommendationRequest(RecommendationType.UPSELL, 37L, 3));

        assertThat(response.source()).isEqualTo(RecommendationService.SOURCE_FALLBACK);
        ArgumentCaptor<ProductSearchRequest> filter = ArgumentCaptor.forClass(ProductSearchRequest.class);
        verify(productService).search(filter.capture(), any(Pageable.class));
        assertThat(filter.getValue().categoryId()).isEqualTo(4);
        assertThat(filter.getValue().minPrice()).isEqualByComparingTo("19990001");
    }

    @Test
    void productTypeWithoutProduct_isRejected() {
        assertThatThrownBy(() -> service.recommend(null, new RecommendationRequest(RecommendationType.SIMILAR, null, 4)))
                .isInstanceOf(BusinessException.class);
    }

    private static ProductResponse product(Long id) {
        ProductResponse product = mock(ProductResponse.class);
        when(product.id()).thenReturn(id);
        return product;
    }
}
