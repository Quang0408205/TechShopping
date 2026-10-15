package com.example.Tech.service.recommendation;

import com.example.Tech.dto.request.recommendation.InteractionRequest;
import com.example.Tech.entity.product.Product;
import com.example.Tech.entity.recommendation.InteractionType;
import com.example.Tech.entity.recommendation.UserInteraction;
import com.example.Tech.exception.BusinessException;
import com.example.Tech.exception.ResourceNotFoundException;
import com.example.Tech.repository.product.ProductRepository;
import com.example.Tech.repository.recommendation.UserInteractionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InteractionRecorderTest {

    private static final Long USER_ID = 7L;
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-13T03:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh"));
    private static final LocalDateTime NOW = LocalDateTime.now(CLOCK);

    @Mock
    private UserInteractionRepository interactionRepository;

    @Mock
    private ProductRepository productRepository;

    private InteractionRecorder recorder;

    @BeforeEach
    void setUp() {
        recorder = new InteractionRecorder(interactionRepository, productRepository, CLOCK);
    }

    @Test
    void record_savesTheBehaviourWithItsWeight() {
        recorder.record(USER_ID, 11L, InteractionType.ADD_TO_CART);

        ArgumentCaptor<UserInteraction> saved = ArgumentCaptor.forClass(UserInteraction.class);
        verify(interactionRepository).save(saved.capture());
        assertThat(saved.getValue().getType()).isEqualTo(InteractionType.ADD_TO_CART);
        assertThat(saved.getValue().getScore()).isEqualTo(4);
        assertThat(saved.getValue().getOccurredAt()).isEqualTo(NOW);
    }

    @Test
    void record_sameBehaviourWithinTheWindow_isIgnored() {
        when(interactionRepository.existsByUserIdAndProductIdAndTypeAndOccurredAtAfter(
                USER_ID, 11L, InteractionType.VIEW, NOW.minus(InteractionRecorder.REPEAT_WINDOW))).thenReturn(true);

        recorder.record(USER_ID, 11L, InteractionType.VIEW);

        verify(interactionRepository, never()).save(any());
    }

    @Test
    void recordPurchases_oneRowPerDistinctProduct() {
        recorder.recordPurchases(USER_ID, List.of(11L, 12L, 11L));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<UserInteraction>> saved = ArgumentCaptor.forClass(List.class);
        verify(interactionRepository).saveAll(saved.capture());
        assertThat(saved.getValue()).extracting(UserInteraction::getProductId).containsExactly(11L, 12L);
        assertThat(saved.getValue()).allMatch(i -> i.getType() == InteractionType.PURCHASE);
    }

    @Test
    void recordFromClient_purchase_isRejected() {
        assertThatThrownBy(() -> recorder.recordFromClient(USER_ID, new InteractionRequest(11L, InteractionType.PURCHASE)))
                .isInstanceOf(BusinessException.class);
        verify(interactionRepository, never()).save(any());
        verify(interactionRepository, never()).saveAll(anyIterable());
    }

    @Test
    void recordFromClient_unknownProduct_isNotFound() {
        when(productRepository.findByIdAndDeletedAtIsNull(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> recorder.recordFromClient(USER_ID, new InteractionRequest(99L, InteractionType.VIEW)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void recordFromClient_view_isRecorded() {
        when(productRepository.findByIdAndDeletedAtIsNull(11L)).thenReturn(Optional.of(new Product()));

        recorder.recordFromClient(USER_ID, new InteractionRequest(11L, InteractionType.VIEW));

        verify(interactionRepository).save(any(UserInteraction.class));
    }
}
