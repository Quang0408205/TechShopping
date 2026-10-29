package com.example.Tech.repository.loyalty;

import com.example.Tech.entity.loyalty.LoyaltyTier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface LoyaltyTierRepository extends JpaRepository<LoyaltyTier, String> {

    List<LoyaltyTier> findAllByOrderBySortOrderAsc();

    /**
     * Raises every customer to the highest tier their total spent reaches now (after ADMIN lowered a threshold).
     * Tiers never go down, so a raised threshold changes nobody.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "update customer_profiles c set tier_code = best.tier_code, updated_at = current_timestamp "
            + "from (select c2.customer_id, (select t.tier_code from loyalty_tiers t where t.min_spent <= c2.total_spent "
            + "order by t.min_spent desc limit 1) tier_code from customer_profiles c2) best "
            + "where best.customer_id = c.customer_id "
            + "and (select sort_order from loyalty_tiers where tier_code = best.tier_code) "
            + "> (select sort_order from loyalty_tiers where tier_code = c.tier_code)", nativeQuery = true)
    int raiseTiersToSpending();
}
