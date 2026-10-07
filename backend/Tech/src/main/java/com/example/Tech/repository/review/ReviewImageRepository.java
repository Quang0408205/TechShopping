package com.example.Tech.repository.review;

import com.example.Tech.entity.review.ReviewImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewImageRepository extends JpaRepository<ReviewImage, Long> {

    List<ReviewImage> findAllByReview_IdOrderByDisplayOrderAsc(Long reviewId);
}
