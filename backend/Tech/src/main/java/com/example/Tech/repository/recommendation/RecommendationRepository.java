package com.example.Tech.repository.recommendation;

import com.example.Tech.entity.recommendation.Recommendation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecommendationRepository extends JpaRepository<Recommendation, Long> {
}
