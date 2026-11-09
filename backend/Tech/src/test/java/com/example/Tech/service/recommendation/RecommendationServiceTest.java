package com.example.Tech.service.recommendation;

import com.example.Tech.dto.request.product.ProductSearchRequest;
import com.example.Tech.dto.request.recommendation.PreferenceRequest;
import com.example.Tech.dto.request.recommendation.RecommendationRequest;
import com.example.Tech.dto.response.recommendation.RecommendationOverviewResponse;
import com.example.Tech.dto.response.common.PageResponse;
import com.example.Tech.dto.response.product.ProductResponse;
import com.example.Tech.dto.response.recommendation.RecommendationResponse;
import com.example.Tech.entity.recommendation.PriceLevel;
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
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
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

    @Mock
    private PreferenceService preferenceService;

    private RecommendationService service;

    @BeforeEach
    void setUp() {
        service = new RecommendationService(aiClient, productService, recommendationRepository, preferenceService,
                Clock.systemDefaultZone());
    }

    @Test
    void aiAnswer_keepsTheAiOrder_dropsHiddenProducts_andLogsForTheUser() {
        when(preferenceService.answers(7L)).thenReturn(Optional.empty());
        when(aiClient.fetchPersonal(eq(7L), eq(8), anyList(), isNull())).thenReturn(Optional.of(personal("BEHAVIOR",
                new AiRecommendationClient.Item(2L, new BigDecimal("0.91234"), "Tương tự A"),
                new AiRecommendationClient.Item(3L, new BigDecimal("0.5"), "Hidden"),
                new AiRecommendationClient.Item(1L, new BigDecimal("0.4"), "Tương tự B"))));
        List<ProductResponse> visible = List.of(product(2L), product(1L));
        when(productService.getActiveByIds(List.of(2L, 3L, 1L))).thenReturn(visible);

        RecommendationResponse response = service.recommend(7L, new RecommendationRequest(RecommendationType.PERSONAL, null, null));

        assertThat(response.source()).isEqualTo(RecommendationService.SOURCE_AI);
        assertThat(response.basis()).isEqualTo("BEHAVIOR");
        assertThat(response.items()).extracting(i -> i.product().id()).containsExactly(2L, 1L);
        assertThat(response.items().get(0).score()).isEqualByComparingTo("0.912");
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Recommendation>> logged = ArgumentCaptor.forClass(List.class);
        verify(recommendationRepository).saveAll(logged.capture());
        assertThat(logged.getValue()).hasSize(2).allMatch(r -> r.getUserId().equals(7L));
    }

    @Test
    void guest_isNotLogged() {
        when(aiClient.fetchPersonal(eq(null), anyInt(), anyList(), any()))
                .thenReturn(Optional.of(personal("POPULAR", new AiRecommendationClient.Item(2L, BigDecimal.ONE, "Phổ biến"))));
        List<ProductResponse> visible = List.of(product(2L));
        when(productService.getActiveByIds(List.of(2L))).thenReturn(visible);

        service.recommend(null, new RecommendationRequest(RecommendationType.PERSONAL, null, 4));

        verify(recommendationRepository, never()).saveAll(anyIterable());
    }

    @Test
    void customer_sendsTheAnswersSavedInTheAccount_andTheBasisIsPassedOn() {
        PreferenceRequest saved = new PreferenceRequest(List.of(4), List.of(1), List.of(PriceLevel.MID), List.of());
        when(preferenceService.answers(7L)).thenReturn(Optional.of(saved));
        when(aiClient.fetchPersonal(7L, 8, List.of(), saved))
                .thenReturn(Optional.of(personal("PREFERENCES", new AiRecommendationClient.Item(2L, BigDecimal.ONE,
                        "Hợp với lựa chọn của bạn: Samsung"))));
        List<ProductResponse> visible = List.of(product(2L));
        when(productService.getActiveByIds(List.of(2L))).thenReturn(visible);

        RecommendationResponse response = service.recommend(7L, new RecommendationRequest(RecommendationType.PERSONAL,
                null, null, new PreferenceRequest(List.of(5), null, null, null)));   // ignored: the account wins

        assertThat(response.basis()).isEqualTo("PREFERENCES");
        assertThat(response.items().getFirst().reason()).isEqualTo("Hợp với lựa chọn của bạn: Samsung");
    }

    @Test
    void guest_sendsTheAnswersKeptInItsBrowser() {
        PreferenceRequest browser = new PreferenceRequest(List.of(5), null, List.of(PriceLevel.LOW), null);
        when(aiClient.fetchPersonal(null, 4, List.of(), browser)).thenReturn(Optional.of(personal("PREFERENCES")));
        when(productService.getActiveByIds(List.of())).thenReturn(List.of());

        RecommendationResponse response = service.recommend(null,
                new RecommendationRequest(RecommendationType.PERSONAL, null, 4, browser));

        assertThat(response.basis()).isEqualTo("PREFERENCES");
        verify(preferenceService, never()).answers(any());
    }

    @Test
    void aiDown_personalFallback_hasNoBasis() {
        when(aiClient.fetchPersonal(any(), anyInt(), anyList(), any())).thenReturn(Optional.empty());
        PageResponse<ProductResponse> page = PageResponse.from(new PageImpl<>(List.of(product(5L))));
        when(productService.search(any(), any(Pageable.class))).thenReturn(page);

        RecommendationResponse response = service.recommend(null, new RecommendationRequest(RecommendationType.PERSONAL, null, 3));

        assertThat(response.source()).isEqualTo(RecommendationService.SOURCE_FALLBACK);
        assertThat(response.basis()).isNull();
        assertThat(response.items()).hasSize(1);
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

    @Test
    void overview_combinesTheModelTheEvaluationAndTheClickRateOfEachList() {
        when(aiClient.status()).thenReturn(Optional.of(Map.of("users", 357)));
        when(aiClient.evaluation()).thenReturn(Optional.of(Map.of("running", false)));
        RecommendationRepository.OnlineStatsRow personal = mock(RecommendationRepository.OnlineStatsRow.class);
        when(personal.getType()).thenReturn("PERSONAL");
        when(personal.getImpressions()).thenReturn(200L);
        when(personal.getClicks()).thenReturn(18L);
        when(personal.getUsers()).thenReturn(25L);
        when(personal.getConverted()).thenReturn(4L);
        when(recommendationRepository.onlineStats(any())).thenReturn(List.of(personal));

        RecommendationOverviewResponse overview = service.overview(900);

        assertThat(overview.aiAvailable()).isTrue();
        assertThat(overview.model()).containsEntry("users", 357);
        assertThat(overview.days()).isEqualTo(365);
        assertThat(overview.online()).extracting(RecommendationOverviewResponse.OnlineStat::type)
                .containsExactly(RecommendationType.values());
        RecommendationOverviewResponse.OnlineStat first = overview.online().getFirst();
        assertThat(first.clickRate()).isEqualByComparingTo("0.09");
        assertThat(first.conversionRate()).isEqualByComparingTo("0.2222");
        // a list never shown: no rate rather than 0 %
        assertThat(overview.online().get(1).impressions()).isZero();
        assertThat(overview.online().get(1).clickRate()).isNull();
    }

    @Test
    void overview_aiDown_stillReturnsTheOnlineFigures() {
        when(aiClient.status()).thenReturn(Optional.empty());
        when(recommendationRepository.onlineStats(any())).thenReturn(List.of());

        RecommendationOverviewResponse overview = service.overview(30);

        assertThat(overview.aiAvailable()).isFalse();
        assertThat(overview.model()).isNull();
        assertThat(overview.evaluation()).isNull();
        assertThat(overview.online()).hasSize(RecommendationType.values().length);
        verify(aiClient, never()).evaluation();
    }

    private static AiRecommendationClient.PersonalList personal(String basis, AiRecommendationClient.Item... items) {
        return new AiRecommendationClient.PersonalList(List.of(items), basis);
    }

    private static ProductResponse product(Long id) {
        ProductResponse product = mock(ProductResponse.class);
        when(product.id()).thenReturn(id);
        return product;
    }
}
