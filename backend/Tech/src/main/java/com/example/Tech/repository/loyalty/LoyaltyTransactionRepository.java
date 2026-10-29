package com.example.Tech.repository.loyalty;

import com.example.Tech.entity.loyalty.LoyaltyTransaction;
import com.example.Tech.entity.loyalty.LoyaltyTransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoyaltyTransactionRepository extends JpaRepository<LoyaltyTransaction, Long> {

    /** A customer's history, newest first, with the order (for its code). */
    @EntityGraph(attributePaths = "order")
    Page<LoyaltyTransaction> findAllByUserIdOrderByIdDesc(Long userId, Pageable pageable);

    boolean existsByOrderIdAndType(Long orderId, LoyaltyTransactionType type);
}
