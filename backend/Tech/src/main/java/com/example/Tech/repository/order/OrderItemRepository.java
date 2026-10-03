package com.example.Tech.repository.order;

import com.example.Tech.entity.order.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    /** Items of several orders with their variant and product, in one query (order, then line order). */
    @Query("select i from OrderItem i join fetch i.variant v join fetch v.product "
            + "where i.order.id in :orderIds order by i.order.id, i.id")
    List<OrderItem> findAllWithProductByOrderIdIn(Collection<Long> orderIds);

    /** A variant that appears in any order cannot be hard-deleted (order_items.variant_id has no cascade). */
    boolean existsByVariantId(Long variantId);
}
