package com.example.Tech.repository.recommendation;

import com.example.Tech.entity.recommendation.Recommendation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface RecommendationRepository extends JpaRepository<Recommendation, Long> {

    /**
     * The user clicked a product in a list of this type: marks the latest impression of that product since
     * {@code since} that is not clicked yet. Returns 0 when there is none (list not logged, e.g. guest popular list).
     */
    @Modifying
    @Query(value = """
            update recommendations set clicked_at = :now
            where recommendation_id = (
                select r.recommendation_id from recommendations r
                where r.user_id = :userId and r.product_id = :productId and r.recommendation_type = :type
                  and r.created_at >= :since and r.clicked_at is null
                order by r.created_at desc
                limit 1)
            """, nativeQuery = true)
    int markClicked(Long userId, Long productId, String type, LocalDateTime since, LocalDateTime now);

    /** Impressions, clicks, users and clicks followed by add-to-cart / purchase of that product within 7 days. */
    interface OnlineStatsRow {
        String getType();

        long getImpressions();

        long getClicks();

        long getUsers();

        long getConverted();
    }

    @Query(value = """
            select r.recommendation_type as type,
                   count(*) as impressions,
                   count(r.clicked_at) as clicks,
                   count(distinct r.user_id) as users,
                   count(*) filter (where r.clicked_at is not null and exists (
                       select 1 from user_interactions ui
                       where ui.user_id = r.user_id and ui.product_id = r.product_id
                         and ui.interaction_type in ('ADD_TO_CART', 'PURCHASE')
                         and ui.timestamp >= r.clicked_at and ui.timestamp < r.clicked_at + interval '7 days')) as converted
            from recommendations r
            where r.created_at >= :since
            group by r.recommendation_type
            """, nativeQuery = true)
    List<OnlineStatsRow> onlineStats(LocalDateTime since);
}
