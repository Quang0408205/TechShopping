package com.example.Tech.service.recommendation;

import com.example.Tech.dto.request.recommendation.InteractionRequest;
import com.example.Tech.entity.recommendation.InteractionType;
import com.example.Tech.entity.recommendation.UserInteraction;
import com.example.Tech.exception.BusinessException;
import com.example.Tech.exception.ErrorCode;
import com.example.Tech.exception.ResourceNotFoundException;
import com.example.Tech.repository.product.ProductRepository;
import com.example.Tech.repository.recommendation.UserInteractionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collection;

/**
 * Writes user_interactions, the behaviour data the recommendation models learn from (report section 3.5.7).
 * The same behaviour on the same product within {@link #REPEAT_WINDOW} is recorded once, so reloading a
 * page does not inflate the score; purchases are always recorded.
 */
@Service
@RequiredArgsConstructor
public class InteractionRecorder {

    static final Duration REPEAT_WINDOW = Duration.ofMinutes(30);

    private final UserInteractionRepository interactionRepository;
    private final ProductRepository productRepository;
    private final Clock clock;

    /** POST /api/v1/interactions: only VIEW / CLICK, on a product that exists. */
    @Transactional
    public void recordFromClient(Long userId, InteractionRequest request) {
        if (!request.type().clientReportable()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "Only VIEW and CLICK can be reported; %s is recorded by the server".formatted(request.type()));
        }
        productRepository.findByIdAndDeletedAtIsNull(request.productId())
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.PRODUCT_NOT_FOUND, request.productId()));
        record(userId, request.productId(), request.type());
    }

    @Transactional
    public void record(Long userId, Long productId, InteractionType type) {
        LocalDateTime now = LocalDateTime.now(clock);
        if (type != InteractionType.PURCHASE && interactionRepository
                .existsByUserIdAndProductIdAndTypeAndOccurredAtAfter(userId, productId, type, now.minus(REPEAT_WINDOW))) {
            return;
        }
        interactionRepository.save(new UserInteraction(userId, productId, type, now));
    }

    @Transactional
    public void recordPurchases(Long userId, Collection<Long> productIds) {
        LocalDateTime now = LocalDateTime.now(clock);
        interactionRepository.saveAll(productIds.stream()
                .distinct()
                .map(productId -> new UserInteraction(userId, productId, InteractionType.PURCHASE, now))
                .toList());
    }
}
