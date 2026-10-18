package com.example.Tech.repository.product;

import com.example.Tech.entity.product.Need;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NeedRepository extends JpaRepository<Need, Integer> {

    List<Need> findAllByCategoryIdOrderByDisplayOrderAscIdAsc(Integer categoryId);

    Optional<Need> findByCategoryIdAndSlug(Integer categoryId, String slug);
}
