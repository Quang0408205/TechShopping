package com.example.Tech.service.combo;

import com.example.Tech.dto.response.combo.ComboOptionResponse;
import com.example.Tech.dto.response.combo.FeaturedComboResponse;
import com.example.Tech.dto.response.product.ProductResponse;
import com.example.Tech.entity.combo.Combo;
import com.example.Tech.entity.combo.ComboItem;
import com.example.Tech.entity.product.Product;
import com.example.Tech.entity.product.ProductImage;
import com.example.Tech.entity.product.ProductVariant;
import com.example.Tech.exception.ErrorCode;
import com.example.Tech.exception.ResourceNotFoundException;
import com.example.Tech.mapper.cart.CartMapper;
import com.example.Tech.repository.combo.ComboRepository;
import com.example.Tech.repository.product.ProductImageRepository;
import com.example.Tech.repository.product.ProductRepository;
import com.example.Tech.repository.product.ProductVariantRepository;
import com.example.Tech.service.inventory.StockAvailability;
import com.example.Tech.service.product.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Public: the combos a product page offers ("Mua kèm tiết kiệm"). */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ComboQueryService {

    private final ComboRepository comboRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final ProductImageRepository imageRepository;
    private final StockAvailability stockAvailability;
    private final ProductService productService;
    private final CartMapper cartMapper;
    private final Clock clock;

    public List<ComboOptionResponse> forProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .filter(found -> found.getDeletedAt() == null && !Boolean.FALSE.equals(found.getActive()))
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.PRODUCT_NOT_FOUND, productId));
        List<Combo> combos = comboRepository.findRunningByMainProductIn(LocalDateTime.now(clock), List.of(product.getId()));
        if (combos.isEmpty()) {
            return List.of();
        }
        List<Long> addOnIds = combos.stream().flatMap(combo -> combo.getItems().stream())
                .map(item -> item.getProduct().getId()).distinct().toList();
        Map<Long, String> images = new HashMap<>();
        for (ProductImage image : imageRepository.findAllByProductIdInBestFirst(addOnIds)) {
            images.putIfAbsent(image.getProduct().getId(), image.getImageUrl());
        }
        List<ProductVariant> variants = variantRepository.findAllByProductIdInOrderByIdAsc(addOnIds);
        Map<Long, Integer> stock = stockAvailability.byVariant(variants.stream().map(ProductVariant::getId).toList());
        Map<Long, List<ProductVariant>> variantsByProduct = new HashMap<>();
        variants.forEach(variant ->
                variantsByProduct.computeIfAbsent(variant.getProduct().getId(), id -> new ArrayList<>()).add(variant));

        List<ComboOptionResponse> result = new ArrayList<>();
        for (Combo combo : combos) {
            List<ComboOptionResponse.Item> items = new ArrayList<>();
            List<ComboItem> sorted = combo.getItems().stream()
                    .sorted(Comparator.comparing(item -> item.getProduct().getId())).toList();
            for (ComboItem item : sorted) {
                Product addOn = item.getProduct();
                if (addOn.getDeletedAt() != null || Boolean.FALSE.equals(addOn.getActive())) {
                    continue;
                }
                ComboCalculator.Rule rule = new ComboCalculator.Rule(item.getDiscountType(), item.getDiscountValue());
                List<ComboOptionResponse.Variant> options = variantsByProduct.getOrDefault(addOn.getId(), List.of()).stream()
                        .filter(cartMapper::isPurchasable)
                        .map(variant -> {
                            BigDecimal price = cartMapper.unitPrice(variant);
                            return new ComboOptionResponse.Variant(variant.getId(), label(variant), variant.getColor(), price,
                                    price.subtract(rule.on(price)), stock.getOrDefault(variant.getId(), 0));
                        })
                        .sorted(Comparator.comparing(ComboOptionResponse.Variant::price))
                        .toList();
                if (!options.isEmpty()) {
                    items.add(new ComboOptionResponse.Item(addOn.getId(), addOn.getName(), addOn.getSlug(),
                            images.get(addOn.getId()), item.getDiscountType().name(), item.getDiscountValue(), options));
                }
            }
            if (!items.isEmpty()) {
                result.add(new ComboOptionResponse(combo.getId(), combo.getName(), combo.getDescription(), combo.getEndsAt(), items));
            }
        }
        return result;
    }

    /**
     * The home page "Combo nổi bật": the newest running combos whose main product is on sale and in stock and that
     * still have a purchasable add-on; each add-on at its cheapest variant (same rules as the product page).
     */
    public List<FeaturedComboResponse> featured(int limit) {
        List<Combo> candidates = comboRepository.findRunning(LocalDateTime.now(clock), PageRequest.of(0, limit * 4));
        List<FeaturedComboResponse> result = new ArrayList<>();
        for (Combo combo : candidates) {
            if (result.size() >= limit) {
                break;
            }
            Long mainId = combo.getMainProduct().getId();
            List<ProductResponse> main = productService.getActiveByIds(List.of(mainId));
            if (main.isEmpty() || main.get(0).availableQuantity() == null || main.get(0).availableQuantity() <= 0
                    || main.get(0).effectivePrice() == null || main.get(0).effectivePrice().signum() <= 0) {
                continue;
            }
            ComboOptionResponse option = forProduct(mainId).stream()
                    .filter(found -> found.id().equals(combo.getId())).findFirst().orElse(null);
            if (option == null) {
                continue;
            }
            BigDecimal original = main.get(0).effectivePrice();
            BigDecimal discounted = original;
            List<FeaturedComboResponse.AddOn> items = new ArrayList<>();
            for (ComboOptionResponse.Item item : option.items()) {
                ComboOptionResponse.Variant cheapest = item.variants().get(0);
                items.add(new FeaturedComboResponse.AddOn(item.productId(), item.name(), item.imageUrl(),
                        cheapest.price(), cheapest.comboPrice()));
                original = original.add(cheapest.price());
                discounted = discounted.add(cheapest.comboPrice());
            }
            result.add(new FeaturedComboResponse(combo.getId(), combo.getName(), combo.getEndsAt(),
                    new FeaturedComboResponse.Product(mainId, main.get(0).name(), main.get(0).primaryImageUrl(),
                            main.get(0).effectivePrice()),
                    items, original, discounted, original.subtract(discounted)));
        }
        return result;
    }

    private static String label(ProductVariant variant) {
        for (String text : new String[] {variant.getStorage(), variant.getRam(), variant.getVariantName()}) {
            if (text != null && !text.isBlank()) {
                return text.trim();
            }
        }
        return "Mặc định";
    }
}
