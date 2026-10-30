package com.example.Tech.service.combo;

import com.example.Tech.entity.cart.CartItem;
import com.example.Tech.entity.combo.Combo;
import com.example.Tech.entity.combo.ComboItem;
import com.example.Tech.entity.order.OrderItem;
import com.example.Tech.mapper.cart.CartMapper;
import com.example.Tech.repository.combo.ComboRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Loads the combos running now for the products of a cart / order and lets {@link ComboCalculator}
 * decide. Cart and checkout use the same code, so the cart total is the order total.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ComboPricingService {

    private final ComboRepository comboRepository;
    private final CartMapper cartMapper;
    private final Clock clock;

    /** Purchasable cart lines only (key = cart line id). */
    public ComboCalculator.Result priceCart(List<CartItem> items) {
        return price(items.stream()
                .filter(item -> cartMapper.isPurchasable(item.getVariant()))
                .map(item -> new ComboCalculator.Line(item.getId(), item.getVariant().getProduct().getId(),
                        cartMapper.unitPrice(item.getVariant()), item.getQuantity()))
                .toList());
    }

    /** Order items at their checkout prices (key = index in the list). */
    public ComboCalculator.Result priceOrderItems(List<OrderItem> items) {
        List<ComboCalculator.Line> lines = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            OrderItem item = items.get(i);
            lines.add(new ComboCalculator.Line((long) i, item.getVariant().getProduct().getId(), item.getUnitPrice(),
                    item.getQuantity()));
        }
        return price(lines);
    }

    public ComboCalculator.Result price(List<ComboCalculator.Line> lines) {
        if (lines.isEmpty()) {
            return ComboCalculator.evaluate(List.of(), lines);
        }
        Set<Long> productIds = new HashSet<>();
        lines.forEach(line -> productIds.add(line.productId()));
        List<ComboCalculator.Combo> combos = comboRepository
                .findRunningByMainProductIn(LocalDateTime.now(clock), productIds).stream()
                .map(combo -> toCalculatorCombo(combo, productIds))
                .filter(combo -> !combo.addOns().isEmpty())
                .toList();
        return ComboCalculator.evaluate(combos, lines);
    }

    private static ComboCalculator.Combo toCalculatorCombo(Combo combo, Set<Long> inCart) {
        Map<Long, ComboCalculator.Rule> addOns = new LinkedHashMap<>();
        for (ComboItem item : combo.getItems()) {
            Long productId = item.getProduct().getId();
            if (inCart.contains(productId) && item.getProduct().getDeletedAt() == null) {
                addOns.put(productId, new ComboCalculator.Rule(item.getDiscountType(), item.getDiscountValue()));
            }
        }
        return new ComboCalculator.Combo(combo.getId(), combo.getName(), combo.getMainProduct().getId(), addOns);
    }
}
