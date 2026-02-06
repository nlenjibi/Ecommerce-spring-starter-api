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
 * Enhanced WishlistRepository interface for JDBC-based implementation
 * Supports both authenticated users and guest sessions
 */
public interface WishlistRepository {

    // ==================== Basic CRUD Operations ====================

    WishlistItem save(WishlistItem wishlistItem);

    List<WishlistItem> saveAll(Iterable<WishlistItem> wishlistItems);

    Optional<WishlistItem> findById(Long id);

    boolean existsById(Long id);

    List<WishlistItem> findAll();

    List<WishlistItem> findAll(Sort sort);

    Page<WishlistItem> findAll(Pageable pageable);

    List<WishlistItem> findAllById(Iterable<Long> ids);

    long count();

    void deleteById(Long id);

    void delete(WishlistItem wishlistItem);

    void deleteAllById(Iterable<Long> ids);

    void deleteAll(Iterable<WishlistItem> wishlistItems);

    void deleteAll();

    // ==================== User Wishlist Queries ====================

    List<WishlistItem> findByUserIdOrderByCreatedAtDesc(Long userId);

    Page<WishlistItem> findByUserId(Long userId, Pageable pageable);

    Optional<WishlistItem> findByUserIdAndProductId(Long userId, Long productId);

    boolean existsByUserIdAndProductId(Long userId, Long productId);

    void deleteByUserId(Long userId);

    void deleteByUserIdAndProductId(Long userId, Long productId);

    Long countByUserId(Long userId);

    Long countUnpurchasedByUserId(Long userId);

    // ==================== Guest Wishlist Queries ====================

    List<WishlistItem> findByGuestSessionIdOrderByCreatedAtDesc(String guestSessionId);

    Optional<WishlistItem> findByGuestSessionIdAndProductId(String guestSessionId, Long productId);

    boolean existsByGuestSessionIdAndProductId(String guestSessionId, Long productId);

    void deleteByGuestSessionId(String guestSessionId);

    void deleteByGuestSessionIdAndProductId(String guestSessionId, Long productId);

    Long countByGuestSessionId(String guestSessionId);

    List<WishlistItem> findExpiredGuestSessions(LocalDateTime currentTime);

    void deleteExpiredGuestSessions(LocalDateTime currentTime);

    // ==================== Price & Stock Tracking ====================

    List<WishlistItem> findItemsWithPriceDrops(Long userId);

    List<WishlistItem> findItemsNeedingStockNotification(Long userId);

    List<WishlistItem> findItemsNeedingPriceNotification(Long userId);

    List<WishlistItem> findItemsBelowTargetPrice(Long userId);

    List<WishlistItem> findItemsForPriceUpdate();

    // ==================== Priority & Organization ====================

    List<WishlistItem> findByUserIdAndPriority(Long userId, WishlistPriority priority);

    List<WishlistItem> findByUserIdAndPriorityOrderByCreatedAtDesc(Long userId, WishlistPriority priority);

    List<WishlistItem> findByUserIdAndCollectionName(Long userId, String collectionName);

    List<String> findDistinctCollectionsByUserId(Long userId);

    List<WishlistItem> findByUserIdAndTagsContaining(Long userId, String tag);

    // ==================== Purchase Status ====================

    List<WishlistItem> findUnpurchasedByUserId(Long userId);

    List<WishlistItem> findPurchasedByUserId(Long userId);

    List<WishlistItem> findPurchasedByUserIdAndDateRange(Long userId, LocalDateTime startDate, LocalDateTime endDate);

    boolean markAsPurchased(Long wishlistItemId);

    void markMultipleAsPurchased(List<Long> wishlistItemIds);

    // ==================== Public & Sharing ====================

    List<WishlistItem> findPublicItemsByUserId(Long userId);

    List<WishlistItem> findPublicItemsByUserIdOrderByCreatedAtDesc(Long userId);

    void updatePublicStatus(Long userId, boolean isPublic);

    void updatePublicStatusForItems(List<Long> itemIds, boolean isPublic);

    // ==================== Reminders ====================

    List<WishlistItem> findItemsWithDueReminders(LocalDateTime currentTime);

    List<WishlistItem> findByUserIdAndReminderEnabled(Long userId, boolean reminderEnabled);

    void updateReminder(Long itemId, LocalDateTime reminderDate, boolean reminderEnabled);

    void cancelReminder(Long itemId);

    // ==================== Product-based Queries ====================

    List<WishlistItem> findByProductId(Long productId);

    Long countByProductId(Long productId);

    List<WishlistItem> findByProductIdIn(List<Long> productIds);

    // ==================== Batch Updates ====================

    boolean updateNotes(Long wishlistItemId, String notes);

    boolean updatePriority(Long wishlistItemId, WishlistPriority priority);

    boolean updatePriceNotificationSettings(Long wishlistItemId, boolean notifyOnPriceDrop, BigDecimal targetPrice);

    boolean updateStockNotificationSetting(Long wishlistItemId, boolean notifyOnStock);

    void updateCollection(List<Long> itemIds, String collectionName);

    void updateTags(Long itemId, String tags);

    // ==================== Analytics Queries ====================

    List<WishlistItem> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);

    List<WishlistItem> findByUserIdAndCreatedAtBetween(Long userId, LocalDateTime startDate, LocalDateTime endDate);

    Long countByUserIdAndCreatedAtAfter(Long userId, LocalDateTime date);

    List<Object[]> findCategoryAnalyticsByUserId(Long userId);

    Double calculateAveragePriceDropByUserId(Long userId);

    Double calculateTotalSavingsByUserId(Long userId);

    // ==================== Guest to User Migration ====================

    /**
     * Transfer all guest wishlist items to a user account
     */
    void transferGuestItemsToUser(String guestSessionId, Long userId);

    /**
     * Merge guest items with existing user wishlist (avoid duplicates)
     */
    void mergeGuestItemsToUser(String guestSessionId, Long userId);

    // ==================== Bulk Operations ====================

    List<WishlistItem> findByUserIdAndProductIdIn(Long userId, List<Long> productIds);

    void deleteByUserIdAndProductIdIn(Long userId, List<Long> productIds);

    int bulkUpdatePriority(List<Long> itemIds, WishlistPriority priority);

    int bulkUpdateCollection(Long userId, List<Long> productIds, String collectionName);

    // ==================== Search & Filter ====================

    List<WishlistItem> searchByUserIdAndKeyword(Long userId, String keyword);

    Page<WishlistItem> findByUserIdWithFilters(
            Long userId,
            WishlistPriority priority,
            Boolean purchased,
            Boolean inStock,
            String collectionName,
            Pageable pageable
    );

    // ==================== Scheduled Updates ====================

    /**
     * Get all user IDs that have at least one wishlist item
     */
    List<Long> findAllUserIdsWithWishlists();
}