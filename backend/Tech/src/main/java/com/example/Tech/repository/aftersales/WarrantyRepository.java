package com.example.Tech.repository.aftersales;

import com.example.Tech.entity.aftersales.Warranty;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface WarrantyRepository extends JpaRepository<Warranty, Long> {

    Optional<Warranty> findByOrderItem_Id(Long orderItemId);

    List<Warranty> findAllByOrderItem_IdIn(Collection<Long> orderItemIds);
}
