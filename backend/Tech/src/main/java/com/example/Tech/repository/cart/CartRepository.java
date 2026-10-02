package com.example.Tech.repository.cart;

import com.example.Tech.entity.cart.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {

    Optional<Cart> findByUserId(Long userId);

    /**
     * Creates the cart of a user if it does not exist yet. ON CONFLICT makes two concurrent first adds
     * safe (carts.user_id is UNIQUE). Returns 1 when a cart was created, 0 when it already existed.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "insert into carts (user_id) values (:userId) on conflict (user_id) do nothing",
            nativeQuery = true)
    int insertIfMissing(Long userId);

    /** Sets updated_at after a change to the items (adding an item does not make the Cart entity dirty). */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "update carts set updated_at = current_timestamp where cart_id = :cartId", nativeQuery = true)
    int touch(Long cartId);
}
