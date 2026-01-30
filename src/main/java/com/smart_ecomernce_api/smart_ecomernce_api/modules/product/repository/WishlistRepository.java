package com.smart_ecomernce_api.smart_ecomernce_api.modules.product.repository;

import com.smart_ecomernce_api.smart_ecomernce_api.modules.product.entity.WishlistItem;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.product.entity.WishlistPriority;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Custom WishlistRepository interface for JDBC-based implementation
 * No JPA dependencies - pure JDBC operations
 */
public interface WishlistRepository {

    // ==================== Basic CRUD Operations ====================

    /**
     * Save a wishlist item (insert if new, update if existing)
     */
    WishlistItem save(WishlistItem wishlistItem);

    /**
     * Save multiple wishlist items
     */
    List<WishlistItem> saveAll(Iterable<WishlistItem> wishlistItems);

    /**
     * Find wishlist item by ID
     */
    Optional<WishlistItem> findById(Long id);

    /**
     * Check if wishlist item exists by ID
     */
    boolean existsById(Long id);

    /**
     * Find all wishlist items
     */
    List<WishlistItem> findAll();

    /**
     * Find all wishlist items with sorting
     */
    List<WishlistItem> findAll(Sort sort);

    /**
     * Find all wishlist items with pagination
     */
    Page<WishlistItem> findAll(Pageable pageable);

    /**
     * Find all wishlist items by IDs
     */
    List<WishlistItem> findAllById(Iterable<Long> ids);

    /**
     * Count total number of wishlist items
     */
    long count();

    /**
     * Delete wishlist item by ID
     */
    void deleteById(Long id);

    /**
     * Delete a wishlist item
     */
    void delete(WishlistItem wishlistItem);

    /**
     * Delete wishlist items by IDs
     */
    void deleteAllById(Iterable<Long> ids);

    /**
     * Delete multiple wishlist items
     */
    void deleteAll(Iterable<WishlistItem> wishlistItems);

    /**
     * Delete all wishlist items
     */
    void deleteAll();

    // ==================== Custom Query Methods ====================

    /**
     * Find all wishlist items for a user, ordered by creation date descending
     */
    List<WishlistItem> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * Find all wishlist items for a user with pagination
     */
    Page<WishlistItem> findByUserId(Long userId, Pageable pageable);

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
    List<WishlistItem> findItemsWithPriceDrops(Long userId);

    /**
     * Find items that need stock notification
     */
    List<WishlistItem> findItemsNeedingStockNotification(Long userId);

    /**
     * Find items that need price drop notification
     */
    List<WishlistItem> findItemsNeedingPriceNotification(Long userId);

    /**
     * Delete all wishlist items for a user
     */
    void deleteByUserId(Long userId);

    /**
     * Find wishlist items by priority
     */
    List<WishlistItem> findByUserIdAndPriority(Long userId, WishlistPriority priority);

    /**
     * Find unpurchased wishlist items for a user
     */
    List<WishlistItem> findUnpurchasedByUserId(Long userId);

    /**
     * Find purchased wishlist items for a user
     */
    List<WishlistItem> findPurchasedByUserId(Long userId);

    /**
     * Count wishlist items for a user
     */
    Long countByUserId(Long userId);

    /**
     * Count unpurchased items for a user
     */
    Long countUnpurchasedByUserId(Long userId);

    /**
     * Find public wishlist items for a user
     */
    List<WishlistItem> findPublicItemsByUserId(Long userId);

    /**
     * Mark wishlist item as purchased
     */
    boolean markAsPurchased(Long wishlistItemId);

    /**
     * Update wishlist item notes
     */
    boolean updateNotes(Long wishlistItemId, String notes);

    /**
     * Update wishlist item priority
     */
    boolean updatePriority(Long wishlistItemId, WishlistPriority priority);

    /**
     * Update price notification settings
     */
    boolean updatePriceNotificationSettings(Long wishlistItemId, boolean notifyOnPriceDrop, BigDecimal targetPrice);

    /**
     * Update stock notification setting
     */
    boolean updateStockNotificationSetting(Long wishlistItemId, boolean notifyOnStock);

    /**
     * Find wishlist items created between dates
     */
    List<WishlistItem> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Find wishlist items for specific product
     */
    List<WishlistItem> findByProductId(Long productId);

    /**
     * Count users who have product in wishlist
     */
    Long countByProductId(Long productId);

    /**
     * Delete wishlist item by user and product
     */
    void deleteByUserIdAndProductId(Long userId, Long productId);
}