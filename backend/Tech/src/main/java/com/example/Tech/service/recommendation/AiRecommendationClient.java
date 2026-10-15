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

/** Calls the Recommendation Service of AI_Service (report table 3.23). Empty when it does not answer. */
@Slf4j
@Component
public class AiRecommendationClient {

    public record Item(Long productId, BigDecimal score, String reason) {
    }

    record ItemsResponse(List<Item> items) {
    }

    private final RestClient restClient;

    public AiRecommendationClient(@Qualifier("aiRestClient") RestClient restClient) {
        this.restClient = restClient;
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
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> stats = restClient.post().uri("/train").retrieve().body(Map.class);
            return Optional.ofNullable(stats);
        } catch (RestClientException exception) {
            log.warn("AI service /train failed: {}", exception.getMessage());
            return Optional.empty();
        }
    }
}
