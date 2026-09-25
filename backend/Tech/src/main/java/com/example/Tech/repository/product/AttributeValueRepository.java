package com.example.Tech.repository.product;

import com.example.Tech.entity.product.AttributeValue;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AttributeValueRepository extends JpaRepository<AttributeValue, Integer> {

    @EntityGraph(attributePaths = "attribute")
    Optional<AttributeValue> findWithAttributeById(Integer id);

    @EntityGraph(attributePaths = "attribute")
    List<AttributeValue> findAllByAttributeIdOrderByValueAsc(Integer attributeId);

    boolean existsByAttributeId(Integer attributeId);

    boolean existsByAttributeIdAndValue(Integer attributeId, String value);

    boolean existsByAttributeIdAndValueAndIdNot(Integer attributeId, String value, Integer id);
}
