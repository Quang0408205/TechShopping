package com.example.Tech.service.inventory;

import com.example.Tech.entity.inventory.Inventory;
import com.example.Tech.entity.store.Store;
import com.example.Tech.repository.inventory.InventoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.ToIntFunction;

/**
 * Plan v3 GĐ E: what customers can buy. Only OPEN stores count, and nothing is reserved: an order takes its
 * stock when staff confirm it (OrderStockLifecycle), so these numbers can be overtaken by a concurrent order and
 * the confirmation still re-checks under a lock.
 */
@Component
@RequiredArgsConstructor
public class StockAvailability {

    private final InventoryRepository inventoryRepository;

    /** productId → units over all open stores (absent ids → 0). */
    public Map<Long, Integer> byProduct(Collection<Long> productIds) {
        return productIds.isEmpty() ? Map.of() : toMap(inventoryRepository.sumOpenStockByProductIds(productIds));
    }

    /** variantId → units over all open stores (absent ids → 0). */
    public Map<Long, Integer> byVariant(Collection<Long> variantIds) {
        return variantIds.isEmpty() ? Map.of() : toMap(inventoryRepository.sumOpenStockByVariantIds(variantIds));
    }

    /** Stock of the given variants in each open store; open stores without any row are absent. */
    public Map<Integer, StoreStock> byStore(Collection<Long> variantIds) {
        Map<Integer, StoreStock> stores = new LinkedHashMap<>();
        if (variantIds.isEmpty()) {
            return stores;
        }
        for (Inventory row : inventoryRepository.findOpenStoreRows(variantIds)) {
            stores.computeIfAbsent(row.getStore().getId(), id -> new StoreStock(row.getStore(), new HashMap<>()))
                    .quantities().put(row.getId().getVariantId(), row.getQuantity());
        }
        return stores;
    }

    /** "Product - Variant (cần 2, còn 1)" for every needed variant with fewer units than needed, by variant id. */
    public static List<String> shortages(Map<Long, Integer> needed, Map<Long, String> labels, ToIntFunction<Long> units) {
        List<String> missing = new ArrayList<>();
        new TreeMap<>(needed).forEach((variantId, quantity) -> {
            int held = units.applyAsInt(variantId);
            if (held < quantity) {
                missing.add("%s (cần %d, còn %d)".formatted(labels.getOrDefault(variantId, "#" + variantId), quantity, held));
            }
        });
        return missing;
    }

    private static Map<Long, Integer> toMap(List<Object[]> rows) {
        Map<Long, Integer> result = new HashMap<>();
        for (Object[] row : rows) {
            result.put((Long) row[0], ((Number) row[1]).intValue());
        }
        return result;
    }

    /** One store's units per variant. */
    public record StoreStock(Store store, Map<Long, Integer> quantities) {

        public int quantityOf(Long variantId) {
            return quantities.getOrDefault(variantId, 0);
        }

        /** Holds every needed unit (variantId → quantity). */
        public boolean holds(Map<Long, Integer> needed) {
            return needed.entrySet().stream().allMatch(entry -> quantityOf(entry.getKey()) >= entry.getValue());
        }

        /** Units of the needed variants in this store (tie-break between equally near stores). */
        public int unitsOf(Map<Long, Integer> needed) {
            return needed.keySet().stream().mapToInt(this::quantityOf).sum();
        }
    }
}
