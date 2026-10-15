package com.example.Tech.repository.order;

import com.example.Tech.entity.order.OrderItem;
import com.example.Tech.entity.order.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    /** Items of several orders with their variant and product, in one query (order, then line order). */
    @Query("select i from OrderItem i join fetch i.variant v join fetch v.product "
            + "where i.order.id in :orderIds order by i.order.id, i.id")
    List<OrderItem> findAllWithProductByOrderIdIn(Collection<Long> orderIds);

    /** A variant that appears in any order cannot be hard-deleted (order_items.variant_id has no cascade). */
    boolean existsByVariantId(Long variantId);

    /** Which of these accounts have a DELIVERED order containing the product ("Đã mua hàng" on reviews). */
    @Query("select distinct i.order.user.id from OrderItem i where i.variant.product.id = :productId "
            + "and i.order.status = :status and i.order.user.id in :userIds")
    List<Long> findBuyerIds(@Param("productId") Long productId, @Param("userIds") Collection<Long> userIds,
                            @Param("status") OrderStatus status);

    /** [userId, productId] pairs among these accounts and products with an order in this status (admin review list). */
    @Query("select distinct i.order.user.id, i.variant.product.id from OrderItem i where i.order.status = :status "
            + "and i.order.user.id in :userIds and i.variant.product.id in :productIds")
    List<Object[]> findBuyerProductPairs(@Param("userIds") Collection<Long> userIds,
                                         @Param("productIds") Collection<Long> productIds,
                                         @Param("status") OrderStatus status);
}
