package com.example.Tech.service.impl.cart;

import com.example.Tech.dto.request.cart.CartItemAddRequest;
import com.example.Tech.dto.request.cart.CartItemUpdateRequest;
import com.example.Tech.dto.response.cart.CartItemResponse;
import com.example.Tech.dto.response.cart.CartResponse;
import com.example.Tech.entity.cart.Cart;
import com.example.Tech.entity.cart.CartItem;
import com.example.Tech.entity.product.ProductImage;
import com.example.Tech.entity.product.ProductVariant;
import com.example.Tech.exception.BusinessException;
import com.example.Tech.exception.ErrorCode;
import com.example.Tech.exception.ResourceNotFoundException;
import com.example.Tech.mapper.cart.CartMapper;
import com.example.Tech.repository.cart.CartItemRepository;
import com.example.Tech.repository.cart.CartRepository;
import com.example.Tech.repository.product.ProductImageRepository;
import com.example.Tech.repository.product.ProductVariantRepository;
import com.example.Tech.service.cart.CartService;
import com.example.Tech.service.user.CurrentUserLoader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductVariantRepository variantRepository;
    private final ProductImageRepository imageRepository;
    private final CurrentUserLoader currentUserLoader;
    private final CartMapper cartMapper;

    @Override
    public CartResponse getCart(Long userId) {
        currentUserLoader.load(userId);
        return cartRepository.findByUserId(userId)
                .map(this::toResponse)
                .orElseGet(CartResponse::empty);
    }

    @Override
    @Transactional
    public CartResponse addItem(Long userId, CartItemAddRequest request) {
        currentUserLoader.load(userId);
        ProductVariant variant = variantRepository.findByIdAndProductDeletedAtIsNull(request.variantId())
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.PRODUCT_VARIANT_NOT_FOUND,
                        request.variantId()));
        if (!cartMapper.isPurchasable(variant)) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_AVAILABLE,
                    "Product variant %d is not available for purchase".formatted(variant.getId()));
        }

        cartRepository.insertIfMissing(userId);
        Long cartId = findCart(userId).getId();
        if (!cartItemRepository.existsByCartIdAndVariantId(cartId, variant.getId())
                && cartItemRepository.countByCartId(cartId) >= MAX_LINES) {
            throw new BusinessException(ErrorCode.CART_LIMIT_EXCEEDED,
                    "A cart can hold at most %d different items".formatted(MAX_LINES));
        }
        cartItemRepository.upsertQuantity(cartId, variant.getId(), request.quantity(), MAX_LINE_QUANTITY);
        cartRepository.touch(cartId);
        log.info("User id={} added {} x variant id={} to cart id={}",
                userId, request.quantity(), variant.getId(), cartId);
        return toResponse(findCart(userId));
    }

    @Override
    @Transactional
    public CartResponse updateItem(Long userId, Long variantId, CartItemUpdateRequest request) {
        currentUserLoader.load(userId);
        Cart cart = findCartContaining(userId, variantId);
        cartItemRepository.updateQuantity(cart.getId(), variantId, request.quantity());
        cartRepository.touch(cart.getId());
        log.info("User id={} set variant id={} to quantity {} in cart id={}",
                userId, variantId, request.quantity(), cart.getId());
        return toResponse(findCart(userId));
    }

    @Override
    @Transactional
    public CartResponse removeItem(Long userId, Long variantId) {
        currentUserLoader.load(userId);
        Cart cart = findCartContaining(userId, variantId);
        cartItemRepository.deleteByCartIdAndVariantId(cart.getId(), variantId);
        cartRepository.touch(cart.getId());
        log.info("User id={} removed variant id={} from cart id={}", userId, variantId, cart.getId());
        return toResponse(findCart(userId));
    }

    @Override
    @Transactional
    public CartResponse clear(Long userId) {
        currentUserLoader.load(userId);
        return cartRepository.findByUserId(userId)
                .map(cart -> {
                    int removed = cartItemRepository.deleteAllByCartId(cart.getId());
                    cartRepository.touch(cart.getId());
                    log.info("User id={} cleared cart id={} ({} lines)", userId, cart.getId(), removed);
                    return toResponse(findCart(userId));
                })
                .orElseGet(CartResponse::empty);
    }

    private Cart findCart(Long userId) {
        return cartRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("Cart of user %d not found".formatted(userId)));
    }

    /** A missing cart or a variant that is not in it → 404 CART_ITEM_NOT_FOUND. */
    private Cart findCartContaining(Long userId, Long variantId) {
        return cartRepository.findByUserId(userId)
                .filter(cart -> cartItemRepository.existsByCartIdAndVariantId(cart.getId(), variantId))
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.CART_ITEM_NOT_FOUND, variantId));
    }

    private CartResponse toResponse(Cart cart) {
        List<CartItem> items = cartItemRepository.findAllWithProductByCartId(cart.getId());
        Map<Long, String> imageByProduct = loadImages(items);
        List<CartItemResponse> responses = items.stream()
                .map(item -> cartMapper.toItemResponse(item,
                        imageByProduct.get(item.getVariant().getProduct().getId())))
                .toList();
        return cartMapper.toResponse(cart, responses);
    }

    /** One image per product (the first of the best-first order), in one query. */
    private Map<Long, String> loadImages(List<CartItem> items) {
        if (items.isEmpty()) {
            return Map.of();
        }
        List<Long> productIds = items.stream()
                .map(item -> item.getVariant().getProduct().getId())
                .distinct()
                .toList();
        Map<Long, String> imageByProduct = new HashMap<>();
        for (ProductImage image : imageRepository.findAllByProductIdInBestFirst(productIds)) {
            imageByProduct.putIfAbsent(image.getProduct().getId(), image.getImageUrl());
        }
        return imageByProduct;
    }
}
