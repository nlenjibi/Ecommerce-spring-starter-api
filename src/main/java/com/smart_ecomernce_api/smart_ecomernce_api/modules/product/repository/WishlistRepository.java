package com.smart_ecomernce_api.smart_ecomernce_api.modules.product.repository;

import com.smart_ecomernce_api.smart_ecomernce_api.modules.product.entity.WishlistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WishlistRepository extends JpaRepository<WishlistItem, Long> {

    /**
     * Find all wishlist items for a user
     */
    List<WishlistItem> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * Find wishlist item by user and product
     */
    Optional<WishlistItem> findByUserIdAndProductId(Long userId, Long productId);

    /**
     * Check if product is in user's wishlist
     */
    boolean existsByUserIdAndProductId(Long userId, Long productId);

    /**
     * Find items with price drops for a user
     */
    @Query("SELECT w FROM WishlistItem w " +
            "WHERE w.user.id = :userId " +
            "AND w.priceWhenAdded > w.product.discountPrice " +
            "AND w.purchased = false " +
            "ORDER BY (w.priceWhenAdded - w.product.discountPrice) DESC")
    List<WishlistItem> findItemsWithPriceDrops(@Param("userId") Long userId);

    /**
     * Find items that need stock notification
     */
    @Query("SELECT w FROM WishlistItem w " +
            "WHERE w.user.id = :userId " +
            "AND w.notifyOnStock = true " +
            "AND w.purchased = false " +
            "AND w.product.inventoryStatus IN ('IN_STOCK', 'LOW_STOCK')")
    List<WishlistItem> findItemsNeedingStockNotification(@Param("userId") Long userId);

    /**
     * Find items that need price drop notification
     */
    @Query("SELECT w FROM WishlistItem w " +
            "WHERE w.user.id = :userId " +
            "AND w.notifyOnPriceDrop = true " +
            "AND w.targetPrice >= w.product.discountPrice " +
            "AND w.purchased = false")
    List<WishlistItem> findItemsNeedingPriceNotification(@Param("userId") Long userId);


    /**
     * Delete all wishlist items for a user
     */
    void deleteByUserId(Long userId);


}