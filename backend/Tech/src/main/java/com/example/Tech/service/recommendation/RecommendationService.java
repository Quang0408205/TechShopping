package com.example.Tech.service.recommendation;

import com.example.Tech.dto.request.product.ProductSearchRequest;
import com.example.Tech.dto.request.recommendation.RecommendationRequest;
import com.example.Tech.dto.response.product.ProductResponse;
import com.example.Tech.dto.response.recommendation.RecommendationOverviewResponse;
import com.example.Tech.dto.response.recommendation.RecommendationResponse;
import com.example.Tech.dto.response.recommendation.RecommendedProductResponse;
import com.example.Tech.entity.recommendation.Recommendation;
import com.example.Tech.entity.recommendation.RecommendationType;
import com.example.Tech.exception.BusinessException;
import com.example.Tech.exception.ErrorCode;
import com.example.Tech.repository.recommendation.RecommendationRepository;
import com.example.Tech.service.product.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * POST /api/v1/recommendations (report section 3.5.7): asks the AI service, checks the products again
 * (still active, not deleted), logs what a logged-in user was shown in recommendations, and falls back to
 * popular / same-category products when the AI service does not answer so shopping is never blocked.
 * Admin: retraining, offline evaluation and the click-through rate of the logged lists.
 */
@Service
@RequiredArgsConstructor
public class RecommendationService {

    public static final String SOURCE_AI = "AI";
    public static final String SOURCE_FALLBACK = "FALLBACK";

    private static final Sort POPULAR_FIRST = Sort.by(Sort.Order.desc("totalReviews"), Sort.Order.desc("id"));

    private final AiRecommendationClient aiClient;
    private final ProductService productService;
    private final RecommendationRepository recommendationRepository;
    private final Clock clock;

    /** Not transactional: no database connection is held while waiting for the AI service. */
    public RecommendationResponse recommend(Long userId, RecommendationRequest request) {
        RecommendationType type = request.type();
        if (type.needsProduct() && request.productId() == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "productId is required for " + type);
        }
        int size = request.sizeOrDefault();
        List<Long> exclude = request.productId() != null ? List.of(request.productId()) : List.of();

        return aiClient.fetch(type, userId, request.productId(), size, exclude)
                .map(items -> fromAi(userId, type, items))
                .orElseGet(() -> new RecommendationResponse(type, SOURCE_FALLBACK, fallback(type, request.productId(), size)));
    }

    public Map<String, Object> train() {
        return aiClient.train().orElseThrow(() -> new BusinessException(ErrorCode.AI_SERVICE_UNAVAILABLE,
                "The AI service did not answer"));
    }

    /**
     * Admin page: the model in use and the latest offline evaluation (from the AI service, null when it is down),
     * plus impressions / clicks / conversions of each list over the last {@code requestedDays} days (1..365).
     */
    public RecommendationOverviewResponse overview(int requestedDays) {
        int days = Math.clamp(requestedDays, 1, 365);
        Map<String, Object> model = aiClient.status().orElse(null);
        Map<String, Object> evaluation = model == null ? null : aiClient.evaluation().orElse(null);
        Map<String, RecommendationRepository.OnlineStatsRow> rows = recommendationRepository
                .onlineStats(LocalDateTime.now(clock).minusDays(days)).stream()
                .collect(Collectors.toMap(RecommendationRepository.OnlineStatsRow::getType, Function.identity()));
        List<RecommendationOverviewResponse.OnlineStat> online = Arrays.stream(RecommendationType.values())
                .map(type -> {
                    RecommendationRepository.OnlineStatsRow row = rows.get(type.name());
                    long impressions = row == null ? 0 : row.getImpressions();
                    long clicks = row == null ? 0 : row.getClicks();
                    long converted = row == null ? 0 : row.getConverted();
                    return new RecommendationOverviewResponse.OnlineStat(type, impressions, clicks,
                            row == null ? 0 : row.getUsers(), converted, rate(clicks, impressions), rate(converted, clicks));
                })
                .toList();
        return new RecommendationOverviewResponse(model != null, model, evaluation, days, online);
    }

    /** Starts the offline evaluation in the AI service; returns its status (running, last result). */
    public Map<String, Object> startEvaluation() {
        return aiClient.startEvaluation().orElseThrow(() -> new BusinessException(ErrorCode.AI_SERVICE_UNAVAILABLE,
                "The AI service did not answer"));
    }

    private static BigDecimal rate(long part, long whole) {
        return whole == 0 ? null : BigDecimal.valueOf(part).divide(BigDecimal.valueOf(whole), 4, RoundingMode.HALF_UP);
    }

    private RecommendationResponse fromAi(Long userId, RecommendationType type, List<AiRecommendationClient.Item> items) {
        Map<Long, AiRecommendationClient.Item> byId = items.stream()
                .collect(Collectors.toMap(AiRecommendationClient.Item::productId, Function.identity(), (a, b) -> a));
        List<RecommendedProductResponse> result = productService
                .getActiveByIds(items.stream().map(AiRecommendationClient.Item::productId).toList())
                .stream()
                .map(product -> {
                    AiRecommendationClient.Item item = byId.get(product.id());
                    return new RecommendedProductResponse(product, scale(item.score()), item.reason());
                })
                .toList();
        if (userId != null && !result.isEmpty()) {
            LocalDateTime now = LocalDateTime.now(clock);
            recommendationRepository.saveAll(result.stream()
                    .map(r -> new Recommendation(userId, r.product().id(), type, r.score(), r.reason(), now))
                    .toList());
        }
        return new RecommendationResponse(type, SOURCE_AI, result);
    }

    /** Popular products (by TGDĐ reviews); for a product page: the same category, more expensive for UPSELL. */
    private List<RecommendedProductResponse> fallback(RecommendationType type, Long productId, int size) {
        if (type == RecommendationType.CROSS_SELL) {
            return List.of();
        }
        ProductSearchRequest filter = new ProductSearchRequest(null, null, null, true, null, null);
        if (type.needsProduct()) {
            ProductResponse anchor = productService.getById(productId);
            // minPrice is inclusive: an upsell must cost strictly more than the product being viewed
            BigDecimal minPrice = type == RecommendationType.UPSELL && anchor.effectivePrice() != null
                    ? anchor.effectivePrice().add(BigDecimal.ONE) : null;
            filter = new ProductSearchRequest(null, anchor.categoryId(), null, true, minPrice, null);
        }
        String reason = type.needsProduct() ? "Cùng danh mục" : "Được nhiều khách hàng quan tâm";
        return productService.search(filter, PageRequest.of(0, size + 1, POPULAR_FIRST)).content().stream()
                .filter(product -> !product.id().equals(productId))
                .limit(size)
                .map(product -> new RecommendedProductResponse(product, null, reason))
                .toList();
    }

    private static BigDecimal scale(BigDecimal score) {
        return score == null ? null : score.setScale(3, RoundingMode.HALF_UP);
    }
}
