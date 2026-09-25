package com.example.Tech.repository.product;

import com.example.Tech.entity.product.VariantAttributeValue;
import com.example.Tech.entity.product.VariantAttributeValueId;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface VariantAttributeValueRepository extends JpaRepository<VariantAttributeValue, VariantAttributeValueId> {

    @EntityGraph(attributePaths = {"attributeValue", "attributeValue.attribute"})
    List<VariantAttributeValue> findAllByVariantIdIn(Collection<Long> variantIds);

    boolean existsByVariantIdAndAttributeValueAttributeId(Long variantId, Integer attributeId);
}
