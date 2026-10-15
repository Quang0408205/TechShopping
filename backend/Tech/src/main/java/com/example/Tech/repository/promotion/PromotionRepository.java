package com.example.Tech.repository.promotion;

import com.example.Tech.entity.promotion.Promotion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface PromotionRepository extends JpaRepository<Promotion, Long>, JpaSpecificationExecutor<Promotion> {

    /** Admin list: the creator is loaded with the promotions (no query per row). */
    @Override
    @EntityGraph(attributePaths = "createdBy")
    Page<Promotion> findAll(Specification<Promotion> spec, Pageable pageable);

    @EntityGraph(attributePaths = "createdBy")
    Optional<Promotion> findWithCreatedById(Long id);
}
