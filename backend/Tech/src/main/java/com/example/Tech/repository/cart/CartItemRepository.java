package com.example.Tech.repository.cart;

import com.example.Tech.entity.cart.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    /** Items of a cart with their variant and product, in the order they were added (one query). */
    @Query("select ci from CartItem ci join fetch ci.variant v join fetch v.product p "
            + "where ci.cart.id = :cartId order by ci.addedAt, ci.id")
    List<CartItem> findAllWithProductByCartId(Long cartId);

    Optional<CartItem> findByCartIdAndVariantId(Long cartId, Long variantId);

    boolean existsByCartIdAndVariantId(Long cartId, Long variantId);

    long countByCartId(Long cartId);

    /**
     * Adds a quantity to the line of a variant, creating the line if needed, and caps the result at
     * maxQuantity. ON CONFLICT makes concurrent adds of the same variant safe (UNIQUE(cart_id, variant_id)).
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "insert into cart_items (cart_id, variant_id, quantity) values (:cartId, :variantId, :quantity) "
            + "on conflict (cart_id, variant_id) "
            + "do update set quantity = least(cart_items.quantity + excluded.quantity, :maxQuantity)",
            nativeQuery = true)
    int upsertQuantity(Long cartId, Long variantId, int quantity, int maxQuantity);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update CartItem ci set ci.quantity = :quantity where ci.cart.id = :cartId and ci.variant.id = :variantId")
    int updateQuantity(Long cartId, Long variantId, int quantity);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from CartItem ci where ci.cart.id = :cartId and ci.variant.id = :variantId")
    int deleteByCartIdAndVariantId(Long cartId, Long variantId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from CartItem ci where ci.cart.id = :cartId")
    int deleteAllByCartId(Long cartId);

    /** Removes a variant from every cart; called before the variant itself is hard-deleted. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from CartItem ci where ci.variant.id = :variantId")
    int deleteAllByVariantId(Long variantId);
}
