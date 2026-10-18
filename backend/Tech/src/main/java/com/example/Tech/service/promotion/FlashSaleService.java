package com.example.Tech.service.promotion;

import com.example.Tech.dto.response.product.ProductResponse;
import com.example.Tech.dto.response.promotion.FlashSaleResponse;
import com.example.Tech.entity.promotion.PromotionProduct;
import com.example.Tech.repository.promotion.PromotionProductRepository;
import com.example.Tech.service.product.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Plan v6 B4, public: the flash sale block of the home page, read from the promotions running right now. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FlashSaleService {

    static final int MAX_ITEMS = 8;

    private final PromotionProductRepository promotionProductRepository;
    private final ProductService productService;
    private final Clock clock;

    /** Up to {@value #MAX_ITEMS} in-stock products that really sell below their base price, deepest discount first. */
    public FlashSaleResponse current() {
        List<PromotionProduct> running = promotionProductRepository.findRunningWithProduct(LocalDateTime.now(clock));
        if (running.isEmpty()) {
            return FlashSaleResponse.empty();
        }
        Map<Long, LocalDateTime> endsByProduct = new HashMap<>();
        for (PromotionProduct row : running) {
            endsByProduct.merge(row.getProduct().getId(), row.getPromotion().getEndDate(),
                    (left, right) -> left.isBefore(right) ? left : right);
        }
        List<ProductResponse> items = productService.getActiveByIds(List.copyOf(endsByProduct.keySet())).stream()
                .filter(product -> product.availableQuantity() != null && product.availableQuantity() > 0)
                .filter(product -> product.basePrice() != null && product.effectivePrice() != null
                        && product.effectivePrice().signum() > 0 && product.effectivePrice().compareTo(product.basePrice()) < 0)
                .sorted(Comparator.comparing(FlashSaleService::depth).reversed().thenComparing(ProductResponse::id))
                .limit(MAX_ITEMS)
                .toList();
        if (items.isEmpty()) {
            return FlashSaleResponse.empty();
        }
        LocalDateTime endsAt = items.stream().map(item -> endsByProduct.get(item.id()))
                .min(Comparator.naturalOrder()).orElseThrow();
        return new FlashSaleResponse(endsAt, items);
    }

    private static BigDecimal depth(ProductResponse product) {
        return product.basePrice().subtract(product.effectivePrice())
                .divide(product.basePrice(), 6, RoundingMode.HALF_UP);
    }
}
