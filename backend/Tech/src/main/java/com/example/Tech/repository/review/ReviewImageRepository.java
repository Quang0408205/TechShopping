package com.example.Tech.repository.review;

import com.example.Tech.entity.review.ReviewImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface ReviewImageRepository extends JpaRepository<ReviewImage, Long> {

    List<ReviewImage> findAllByReview_IdOrderByDisplayOrderAsc(Long reviewId);

    /** Photos of a page of reviews in one query, grouped by review then display order. */
    List<ReviewImage> findAllByReview_IdInOrderByReview_IdAscDisplayOrderAsc(Collection<Long> reviewIds);
}
