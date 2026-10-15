package com.example.Tech.repository.user;

import com.example.Tech.entity.user.CustomerProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;

public interface CustomerProfileRepository extends JpaRepository<CustomerProfile, Long> {

    /**
     * Adds a delivered order's total to the customer's total_spent in one UPDATE (no lost update when two
     * orders are delivered at the same time). Returns 0 when the user has no customer profile.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "update customer_profiles set total_spent = coalesce(total_spent, 0) + :amount, "
            + "updated_at = current_timestamp where customer_id = :userId", nativeQuery = true)
    int addToTotalSpent(Long userId, BigDecimal amount);
}
