package com.example.Tech.repository.review;

import com.example.Tech.entity.review.ReviewLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface ReviewLikeRepository extends JpaRepository<ReviewLike, ReviewLike.Key> {

    /** [reviewId, likes] for these reviews (reviews without a like are absent). */
    @Query("select l.id.reviewId, count(l) from ReviewLike l where l.id.reviewId in :reviewIds group by l.id.reviewId")
    List<Object[]> countByReviewIds(@Param("reviewIds") Collection<Long> reviewIds);

    /** Which of these reviews the account has liked. */
    @Query("select l.id.reviewId from ReviewLike l where l.id.userId = :userId and l.id.reviewId in :reviewIds")
    List<Long> findLikedReviewIds(@Param("userId") Long userId, @Param("reviewIds") Collection<Long> reviewIds);

    @Query("select count(l) from ReviewLike l where l.id.reviewId = :reviewId")
    long countByReviewId(@Param("reviewId") Long reviewId);
}
