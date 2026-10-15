package com.example.Tech.repository.product;

import com.example.Tech.entity.product.Brand;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BrandRepository extends JpaRepository<Brand, Integer> {

    /** Brands of the products on sale in a category (active, not deleted, category visible), by name. */
    @Query("""
            select distinct b from Product p join p.brand b
            where p.category.id = :categoryId and p.active = true and p.deletedAt is null
              and (p.category.active is null or p.category.active = true)
            order by b.name, b.id
            """)
    List<Brand> findOnSaleByCategoryId(@Param("categoryId") Integer categoryId);

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Integer id);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Integer id);
}
