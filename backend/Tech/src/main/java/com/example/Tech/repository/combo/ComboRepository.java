package com.example.Tech.repository.combo;

import com.example.Tech.entity.combo.Combo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ComboRepository extends JpaRepository<Combo, Long>, JpaSpecificationExecutor<Combo> {

    /** Combos running now whose main product is one of {@code productIds}, with their add-ons. */
    @Query("select distinct c from Combo c join fetch c.items i join fetch i.product where c.active = true "
            + "and c.startsAt <= :now and c.endsAt > :now and c.mainProduct.id in :productIds order by c.id")
    List<Combo> findRunningByMainProductIn(LocalDateTime now, Collection<Long> productIds);

    @Query("select c from Combo c join fetch c.mainProduct left join fetch c.items i left join fetch i.product where c.id = :id")
    Optional<Combo> findWithItems(Long id);

    @Query("select count(oc) > 0 from OrderCombo oc where oc.comboId = :comboId")
    boolean usedByOrders(Long comboId);
}
