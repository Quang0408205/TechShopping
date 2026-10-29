package com.example.Tech.repository.wishlist;

import com.example.Tech.entity.wishlist.WishlistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface WishlistItemRepository extends JpaRepository<WishlistItem, Long> {

    List<WishlistItem> findAllByUserIdOrderByIdDesc(Long userId);

    Optional<WishlistItem> findByUserIdAndProductId(Long userId, Long productId);

    long countByUserId(Long userId);

    @Query("select w.productId from WishlistItem w where w.userId = :userId order by w.id desc")
    List<Long> findProductIdsByUserId(Long userId);

    @Modifying
    @Query("delete from WishlistItem w where w.userId = :userId and w.productId = :productId")
    int deleteByUserIdAndProductId(Long userId, Long productId);

    /** Every saved item (the price check walks all of them; the table stays small: at most 200 per customer). */
    @Query("select w from WishlistItem w order by w.userId, w.id")
    List<WishlistItem> findAllForPriceCheck();
}
