package com.example.Tech.repository.inventory;

import com.example.Tech.entity.inventory.Inventory;
import com.example.Tech.entity.inventory.InventoryId;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface InventoryRepository extends JpaRepository<Inventory, InventoryId>, JpaSpecificationExecutor<Inventory> {

    /** Store inventory page: variant and product loaded with the rows (no query per row). */
    @Override
    @EntityGraph(attributePaths = {"variant", "variant.product"})
    Page<Inventory> findAll(Specification<Inventory> spec, Pageable pageable);

    @EntityGraph(attributePaths = {"variant", "variant.product"})
    Page<Inventory> findByIdStoreId(Integer storeId, Pageable pageable);

    Optional<Inventory> findByIdStoreIdAndIdVariantId(Integer storeId, Long variantId);

    List<Inventory> findByIdStoreIdAndIdVariantIdIn(Integer storeId, List<Long> variantIds);

    /** Every store's row for the given variants, with the store (all-stores view). */
    @EntityGraph(attributePaths = "store")
    List<Inventory> findAllByIdVariantIdIn(Collection<Long> variantIds);

    /**
     * Locks the inventory rows of one store for the given variants (SELECT … FOR UPDATE, ordered by
     * variant id so two callers cannot deadlock). A variant with no row yet is simply absent from the
     * result (absent means quantity 0 — callers must not treat that as "nothing to lock").
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Inventory i where i.id.storeId = :storeId and i.id.variantId in :variantIds order by i.id.variantId")
    List<Inventory> findAllForUpdate(@Param("storeId") Integer storeId, @Param("variantIds") List<Long> variantIds);

    /**
     * Creates the (store, variant) row with quantity 0 if it does not exist yet. ON CONFLICT makes two
     * concurrent first stock-ins of the same variant safe; the caller then locks the row with findAllForUpdate.
     */
    @Modifying(flushAutomatically = true)
    @Query(value = "insert into inventory (store_id, variant_id, quantity) values (:storeId, :variantId, 0) "
            + "on conflict (store_id, variant_id) do nothing", nativeQuery = true)
    int insertIfMissing(@Param("storeId") Integer storeId, @Param("variantId") Long variantId);

    boolean existsByIdStoreId(Integer storeId);
}
