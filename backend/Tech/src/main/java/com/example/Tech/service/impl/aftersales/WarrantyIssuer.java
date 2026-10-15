package com.example.Tech.service.impl.aftersales;

import com.example.Tech.entity.aftersales.Warranty;
import com.example.Tech.entity.order.Order;
import com.example.Tech.entity.order.OrderItem;
import com.example.Tech.repository.aftersales.WarrantyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** Runs inside the caller's transaction when an order becomes DELIVERED. */
@Component
@RequiredArgsConstructor
public class WarrantyIssuer {

    private final WarrantyRepository warrantyRepository;

    /** One warranty per order line, from the delivery day for the product's warranty months at that moment. */
    public List<Warranty> issueFor(Order order, LocalDate deliveredOn) {
        List<OrderItem> items = order.getItems();
        if (items.isEmpty()) {
            return List.of();
        }
        Set<Long> alreadyIssued = warrantyRepository
                .findAllByOrderItem_IdIn(items.stream().map(OrderItem::getId).toList()).stream()
                .map(warranty -> warranty.getOrderItem().getId())
                .collect(Collectors.toSet());

        List<Warranty> issued = new ArrayList<>();
        for (OrderItem item : items) {
            Integer months = item.getVariant().getProduct().getWarrantyMonths();
            // 0 months = sold without warranty
            if (months == null || months <= 0 || alreadyIssued.contains(item.getId())) {
                continue;
            }
            Warranty warranty = new Warranty();
            warranty.setOrderItem(item);
            warranty.setStartDate(deliveredOn);
            warranty.setEndDate(deliveredOn.plusMonths(months));
            issued.add(warranty);
        }
        return warrantyRepository.saveAll(issued);
    }
}
