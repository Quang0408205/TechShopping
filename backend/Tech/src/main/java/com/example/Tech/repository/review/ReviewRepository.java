package com.example.Tech.repository.review;

import com.example.Tech.entity.review.Review;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    Optional<Review> findByUser_IdAndProduct_Id(Long userId, Long productId);

    boolean existsByUser_IdAndProduct_Id(Long userId, Long productId);

    List<Review> findAllByProduct_IdAndHiddenFalseOrderByCreatedAtDesc(Long productId);
}
