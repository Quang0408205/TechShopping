package com.example.Tech.service.wishlist;

import com.example.Tech.dto.response.product.ProductResponse;
import com.example.Tech.dto.response.wishlist.WishlistItemResponse;
import com.example.Tech.entity.wishlist.WishlistItem;
import com.example.Tech.exception.BusinessException;
import com.example.Tech.exception.ErrorCode;
import com.example.Tech.exception.ResourceNotFoundException;
import com.example.Tech.repository.wishlist.WishlistItemRepository;
import com.example.Tech.service.product.ProductService;
import com.example.Tech.service.user.CurrentUserLoader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** A customer's saved products. Products taken off sale stay saved but are not listed. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WishlistService {

    public static final int MAX_ITEMS = 200;

    private final WishlistItemRepository repository;
    private final ProductService productService;
    private final CurrentUserLoader currentUserLoader;

    public List<WishlistItemResponse> list(Long userId) {
        currentUserLoader.load(userId);
        List<WishlistItem> items = repository.findAllByUserIdOrderByIdDesc(userId);
        Map<Long, ProductResponse> products = new HashMap<>();
        productService.getActiveByIds(items.stream().map(WishlistItem::getProductId).toList())
                .forEach(product -> products.put(product.id(), product));
        return items.stream()
                .filter(item -> products.containsKey(item.getProductId()))
                .map(item -> toResponse(item, products.get(item.getProductId())))
                .toList();
    }

    public List<Long> ids(Long userId) {
        currentUserLoader.load(userId);
        return repository.findProductIdsByUserId(userId);
    }

    @Transactional
    public void add(Long userId, Long productId) {
        currentUserLoader.load(userId);
        ProductResponse product = productService.getActiveByIds(List.of(productId)).stream().findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.PRODUCT_NOT_FOUND, productId));
        if (repository.findByUserIdAndProductId(userId, productId).isPresent()) {
            return;
        }
        if (repository.countByUserId(userId) >= MAX_ITEMS) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "Danh sách yêu thích tối đa %d sản phẩm".formatted(MAX_ITEMS));
        }
        repository.save(new WishlistItem(userId, productId, priceOf(product)));
    }

    @Transactional
    public void remove(Long userId, Long productId) {
        currentUserLoader.load(userId);
        repository.deleteByUserIdAndProductId(userId, productId);
    }

    /** The price a customer pays now: the promotion-aware price, else the list price. */
    static BigDecimal priceOf(ProductResponse product) {
        return product.effectivePrice() != null ? product.effectivePrice() : product.basePrice();
    }

    private static WishlistItemResponse toResponse(WishlistItem item, ProductResponse product) {
        BigDecimal price = priceOf(product);
        boolean cheaper = product.basePrice() != null && price != null && product.basePrice().compareTo(price) > 0;
        return new WishlistItemResponse(product.id(), product.name(), product.categoryName(), product.primaryImageUrl(),
                price, cheaper ? product.basePrice() : null, item.getPriceAtAdd(),
                product.availableQuantity() != null && product.availableQuantity() > 0, item.getCreatedAt());
    }
}
