package com.example.Tech.repository.product;

import com.example.Tech.entity.product.CategoryAttribute;
import com.example.Tech.entity.product.CategoryAttributeId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CategoryAttributeRepository extends JpaRepository<CategoryAttribute, CategoryAttributeId> {

    /** The attributes of one category, in display order, attribute loaded too. */
    @Query("""
            select ca from CategoryAttribute ca
            join fetch ca.attribute
            where ca.id.categoryId = :categoryId
            order by ca.displayOrder, ca.attribute.name
            """)
    List<CategoryAttribute> findAllByCategoryId(@Param("categoryId") Integer categoryId);
}
