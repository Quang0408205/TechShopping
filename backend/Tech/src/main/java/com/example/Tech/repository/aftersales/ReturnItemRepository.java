package com.example.Tech.repository.aftersales;

import com.example.Tech.entity.aftersales.ReturnItem;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface ReturnItemRepository extends JpaRepository<ReturnItem, Long> {

    /** Quantity per order line already held by a return that is open or refunded: [orderItemId, quantity]. */
    @Query("""
            select ri.orderItem.id, sum(ri.quantity) from ReturnItem ri
            where ri.orderItem.id in :orderItemIds
              and ri.returnRequest.status not in (com.example.Tech.entity.aftersales.ReturnStatus.REJECTED,
                                                  com.example.Tech.entity.aftersales.ReturnStatus.CANCELLED)
            group by ri.orderItem.id
            """)
    List<Object[]> sumHeldQuantities(@Param("orderItemIds") Collection<Long> orderItemIds);

    @EntityGraph(attributePaths = "orderItem.variant.product")
    List<ReturnItem> findAllByReturnRequest_IdIn(Collection<Long> returnRequestIds);
}
