package com.example.Tech.repository.user;

import com.example.Tech.entity.user.CustomerProfile;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.Optional;

public interface CustomerProfileRepository extends JpaRepository<CustomerProfile, Long> {

    /**
     * Adds an amount (a refunded return: negative) to the customer's total_spent in one UPDATE (no lost update when
     * two changes happen at the same time). Returns 0 when the user has no customer profile.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "update customer_profiles set total_spent = coalesce(total_spent, 0) + :amount, "
            + "updated_at = current_timestamp where customer_id = :userId", nativeQuery = true)
    int addToTotalSpent(Long userId, BigDecimal amount);

    /** Row lock while the points balance or the tier changes (two orders never spend the same points). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from CustomerProfile c where c.customerId = :userId")
    Optional<CustomerProfile> findByIdForUpdate(Long userId);
}
