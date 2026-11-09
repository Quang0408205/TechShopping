package com.example.Tech.service.recommendation;

import com.example.Tech.entity.recommendation.RecommendationType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Calls the Recommendation Service of AI_Service. Empty when it does not answer. */
@Slf4j
@Component
public class AiRecommendationClient {

    public record Item(Long productId, BigDecimal score, String reason) {
    }

    record ItemsResponse(List<Item> items) {
    }

    private final RestClient restClient;
    /** Long read timeout: training takes a few seconds, more than the 3 s allowed for a recommendation list. */
    private final RestClient slowClient;

    public AiRecommendationClient(@Qualifier("aiRestClient") RestClient restClient,
                                  @Qualifier("aiChatRestClient") RestClient slowClient) {
        this.restClient = restClient;
        this.slowClient = slowClient;
    }

    public Optional<List<Item>> fetch(RecommendationType type, Long userId, Long productId, int size,
                                      List<Long> excludeProductIds) {
        Map<String, Object> body = new HashMap<>();
        body.put("userId", userId);
        body.put("n", size);
        if (type.needsProduct()) {
            body.put("productId", productId);
        } else {
            body.put("excludeProductIds", excludeProductIds);
        }
        try {
            ItemsResponse response = restClient.post()
                    .uri(type.aiPath())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(ItemsResponse.class);
            return Optional.ofNullable(response).map(ItemsResponse::items);
        } catch (RestClientException exception) {
            log.warn("AI service {} failed, using the fallback list: {}", type.aiPath(), exception.getMessage());
            return Optional.empty();
        }
    }

    /** POST /train: retrains ALS and the product vectors; returns the training statistics. */
    public Optional<Map<String, Object>> train() {
        return call(slowClient.post().uri("/train"), "POST /train");
    }

    /** GET /health: the model in use (trained at, products, users, interactions, features...). */
    public Optional<Map<String, Object>> status() {
        return call(restClient.get().uri("/health"), "GET /health");
    }

    /** GET /evaluation: latest offline evaluation (rows by model / α) and whether one is running. */
    public Optional<Map<String, Object>> evaluation() {
        return call(restClient.get().uri("/evaluation"), "GET /evaluation");
    }

    /** POST /evaluation: starts an offline evaluation in the background (the result is read with GET). */
    public Optional<Map<String, Object>> startEvaluation() {
        return call(restClient.post().uri("/evaluation"), "POST /evaluation");
    }

    @SuppressWarnings("unchecked")
    private Optional<Map<String, Object>> call(RestClient.RequestHeadersSpec<?> request, String label) {
        try {
            return Optional.ofNullable(request.retrieve().body(Map.class));
        } catch (RestClientException exception) {
            log.warn("AI service {} failed: {}", label, exception.getMessage());
            return Optional.empty();
        }
    }
}
