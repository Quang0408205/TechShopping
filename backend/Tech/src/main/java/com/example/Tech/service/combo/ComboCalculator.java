package com.example.Tech.service.combo;

import com.example.Tech.entity.combo.ComboDiscountType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * No I/O: which add-on units of a cart get a combo discount.
 * <ul>
 *   <li>Per (main product, add-on product) the number of discounted add-on units is at most the number of main
 *       units in the cart (2 phones + 2 cases → both cases; 1 phone + 2 cases → one).</li>
 *   <li>A unit plays one role: a unit taken as an add-on is not a main unit, and a main unit backing a discount is
 *       not discounted as an add-on.</li>
 *   <li>An add-on in several combos takes the largest discount; the dearest units are discounted first.</li>
 *   <li>A discount never exceeds the unit price (percent rounds down to whole đồng).</li>
 * </ul>
 */
public final class ComboCalculator {

    private ComboCalculator() {
    }

    public record Rule(ComboDiscountType type, BigDecimal value) {

        public BigDecimal on(BigDecimal unitPrice) {
            BigDecimal raw = type == ComboDiscountType.PERCENT
                    ? unitPrice.multiply(value).divide(BigDecimal.valueOf(100), 0, RoundingMode.FLOOR)
                    : value.setScale(0, RoundingMode.FLOOR);
            return raw.min(unitPrice).max(BigDecimal.ZERO);
        }
    }

    public record Combo(Long id, String name, Long mainProductId, Map<Long, Rule> addOns) {
    }

    /** key = cart / order line, unitPrice = price after product promotions. */
    public record Line(Long key, Long productId, BigDecimal unitPrice, int quantity) {
    }

    public record Applied(Long comboId, String name, int units, BigDecimal discount) {
    }

    public record Result(Map<Long, BigDecimal> lineDiscounts, Map<Long, Set<String>> lineCombos,
                         List<Applied> applied, BigDecimal total) {

        static Result none() {
            return new Result(Map.of(), Map.of(), List.of(), BigDecimal.ZERO);
        }

        public BigDecimal discountOf(Long lineKey) {
            return lineDiscounts.getOrDefault(lineKey, BigDecimal.ZERO);
        }
    }

    private record Unit(Long lineKey, BigDecimal price) {
    }

    private record Pair(Long main, Long addOn) {
    }

    public static Result evaluate(List<Combo> combos, List<Line> lines) {
        if (combos.isEmpty() || lines.isEmpty()) {
            return Result.none();
        }
        Map<Long, List<Unit>> unitsByProduct = new LinkedHashMap<>();
        for (Line line : lines) {
            List<Unit> units = unitsByProduct.computeIfAbsent(line.productId(), id -> new ArrayList<>());
            for (int i = 0; i < line.quantity(); i++) {
                units.add(new Unit(line.key(), line.unitPrice()));
            }
        }
        unitsByProduct.values().forEach(units -> units.sort(Comparator.comparing(Unit::price).reversed()));

        Map<Pair, Integer> slotsUsed = new HashMap<>();
        Map<Long, Integer> takenAsAddOn = new HashMap<>();
        Map<Long, BigDecimal> lineDiscounts = new LinkedHashMap<>();
        Map<Long, Set<String>> lineCombos = new LinkedHashMap<>();
        Map<Long, int[]> appliedUnits = new LinkedHashMap<>();
        Map<Long, BigDecimal> appliedAmount = new LinkedHashMap<>();
        Map<Long, String> comboNames = new HashMap<>();

        for (Map.Entry<Long, List<Unit>> entry : unitsByProduct.entrySet()) {
            Long addOn = entry.getKey();
            for (Unit unit : entry.getValue()) {
                int reservedAsMain = slotsUsed.entrySet().stream()
                        .filter(slot -> slot.getKey().main().equals(addOn))
                        .mapToInt(Map.Entry::getValue).max().orElse(0);
                if (takenAsAddOn.getOrDefault(addOn, 0) + reservedAsMain >= entry.getValue().size()) {
                    break;
                }
                Combo best = null;
                BigDecimal bestDiscount = BigDecimal.ZERO;
                for (Combo combo : combos) {
                    Rule rule = combo.addOns().get(addOn);
                    List<Unit> mainUnits = unitsByProduct.get(combo.mainProductId());
                    if (rule == null || mainUnits == null || combo.mainProductId().equals(addOn)) {
                        continue;
                    }
                    int freeMain = mainUnits.size() - takenAsAddOn.getOrDefault(combo.mainProductId(), 0);
                    if (slotsUsed.getOrDefault(new Pair(combo.mainProductId(), addOn), 0) >= freeMain) {
                        continue;
                    }
                    BigDecimal discount = rule.on(unit.price());
                    if (discount.compareTo(bestDiscount) > 0) {
                        best = combo;
                        bestDiscount = discount;
                    }
                }
                if (best == null) {
                    continue;
                }
                slotsUsed.merge(new Pair(best.mainProductId(), addOn), 1, Integer::sum);
                takenAsAddOn.merge(addOn, 1, Integer::sum);
                lineDiscounts.merge(unit.lineKey(), bestDiscount, BigDecimal::add);
                lineCombos.computeIfAbsent(unit.lineKey(), key -> new LinkedHashSet<>()).add(best.name());
                appliedUnits.computeIfAbsent(best.id(), id -> new int[1])[0]++;
                appliedAmount.merge(best.id(), bestDiscount, BigDecimal::add);
                comboNames.put(best.id(), best.name());
            }
        }
        List<Applied> applied = appliedUnits.entrySet().stream()
                .map(e -> new Applied(e.getKey(), comboNames.get(e.getKey()), e.getValue()[0], appliedAmount.get(e.getKey())))
                .toList();
        BigDecimal total = lineDiscounts.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return new Result(lineDiscounts, lineCombos, applied, total);
    }
}
