package com.example.Tech.service.wishlist;

import com.example.Tech.dto.response.product.ProductResponse;
import com.example.Tech.dto.response.wishlist.PriceAlertRunResponse;
import com.example.Tech.entity.user.User;
import com.example.Tech.entity.wishlist.WishlistItem;
import com.example.Tech.repository.user.UserRepository;
import com.example.Tech.repository.wishlist.WishlistItemRepository;
import com.example.Tech.service.mail.WishlistMailService;
import com.example.Tech.service.mail.WishlistMailTemplates.Drop;
import com.example.Tech.service.product.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Compares the price of every saved product with the item's mark and mails each customer once with the
 * products that got cheaper. The mark goes down after a drop and up when the price rises again, so a later drop is
 * reported again. Run by the hourly job and by ADMIN (demo).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WishlistPriceAlertService {

    private final WishlistItemRepository repository;
    private final ProductService productService;
    private final UserRepository userRepository;
    private final WishlistMailService mailService;

    @Transactional
    public PriceAlertRunResponse run() {
        List<WishlistItem> items = repository.findAllForPriceCheck();
        Map<Long, ProductResponse> products = new HashMap<>();
        productService.getActiveByIds(items.stream().map(WishlistItem::getProductId).distinct().toList())
                .forEach(product -> products.put(product.id(), product));

        int checked = 0;
        int dropped = 0;
        Map<Long, List<Drop>> dropsByUser = new LinkedHashMap<>();
        for (WishlistItem item : items) {
            ProductResponse product = products.get(item.getProductId());
            BigDecimal current = product == null ? null : WishlistService.priceOf(product);
            if (current == null) {
                continue;
            }
            checked++;
            BigDecimal mark = item.getLastAlertPrice() != null ? item.getLastAlertPrice() : item.getPriceAtAdd();
            if (mark == null || current.compareTo(mark) > 0) {
                item.setLastAlertPrice(current);
            } else if (current.compareTo(mark) < 0) {
                dropsByUser.computeIfAbsent(item.getUserId(), id -> new ArrayList<>())
                        .add(new Drop(product.id(), product.name(), mark, current));
                item.setLastAlertPrice(current);
                dropped++;
            }
        }

        int mailed = 0;
        for (Map.Entry<Long, List<Drop>> entry : dropsByUser.entrySet()) {
            User user = userRepository.findById(entry.getKey()).filter(User::isEnabled).orElse(null);
            if (user != null && mailService.sendPriceDrops(user, entry.getValue())) {
                mailed++;
            }
        }
        log.info("Wishlist price check: {} item(s) checked, {} dropped, {} customer(s) mailed", checked, dropped, mailed);
        return new PriceAlertRunResponse(checked, dropped, mailed);
    }
}
