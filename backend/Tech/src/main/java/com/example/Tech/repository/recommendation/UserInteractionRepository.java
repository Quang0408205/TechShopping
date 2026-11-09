package com.example.Tech.repository.recommendation;

import com.example.Tech.entity.recommendation.InteractionType;
import com.example.Tech.entity.recommendation.UserInteraction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface UserInteractionRepository extends JpaRepository<UserInteraction, Long> {

    /** Any view, click, like, cart or purchase: the AI service then recommends from the customer's own activity. */
    boolean existsByUserId(Long userId);


    boolean existsByUserIdAndProductIdAndTypeAndOccurredAtAfter(Long userId, Long productId, InteractionType type,
                                                                LocalDateTime after);
}
