package com.smart_ecomernce_api.smart_ecomernce_api.modules.product.service;

import com.smart_ecomernce_api.smart_ecomernce_api.modules.product.dto.*;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.product.entity.WishlistPriority;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public interface WishlistService {

    // ==================== Basic Operations ====================

    /**
     * Add product to wishlist
     */
    WishlistItemDto addToWishlist(Long userId, AddToWishlistRequest request);

    /**
     * Get user's wishlist
     */
    List<WishlistItemDto> getUserWishlist(Long userId);

    /**
     * Get wishlist with pagination
     */
    Page<WishlistItemDto> getUserWishlistPaginated(Long userId, Pageable pageable);

    /**
     * Get wishlist summary
     */
    WishlistSummaryDto getWishlistSummary(Long userId);

    /**
     * Remove product from wishlist
     */
    void removeFromWishlist(Long userId, Long productId);

    /**
     * Update wishlist item
     */
    WishlistItemDto updateWishlistItem(Long userId, Long productId, UpdateWishlistItemRequest request);

    /**
     * Check if product is in wishlist
     */
    boolean isInWishlist(Long userId, Long productId);

    /**
     * Clear entire wishlist
     */
    void clearWishlist(Long userId);

    // ==================== Price & Stock Tracking ====================

    /**
     * Get items with price drops
     */
    List<WishlistItemDto> getItemsWithPriceDrops(Long userId);

    /**
     * Get items that need stock notification
     */
    List<WishlistItemDto> getItemsNeedingStockNotification(Long userId);

    /**
     * Get items with price below target
     */
    List<WishlistItemDto> getItemsBelowTargetPrice(Long userId);

    /**
     * Update prices for all wishlist items (scheduled task)
     */
    void updateWishlistPrices();

    // ==================== Collections & Organization ====================

    /**
     * Get wishlist items by collection/category
     */
    List<WishlistItemDto> getWishlistByCollection(Long userId, String collectionName);

    /**
     * Get all collections for a user
     */
    List<String> getUserCollections(Long userId);

    /**
     * Move items to collection
     */
    void moveItemsToCollection(Long userId, List<Long> productIds, String collectionName);

    /**
     * Get wishlist items by priority
     */
    List<WishlistItemDto> getWishlistByPriority(Long userId, WishlistPriority priority);

    /**
     * Get wishlist items by tags
     */
    List<WishlistItemDto> getWishlistByTags(Long userId, List<String> tags);

    // ==================== Purchase & Cart Operations ====================

    /**
     * Mark item as purchased
     */
    WishlistItemDto markAsPurchased(Long userId, Long productId);

    /**
     * Mark multiple items as purchased
     */
    void markMultipleAsPurchased(Long userId, List<Long> productIds);

    /**
     * Move wishlist item to cart
     */
    void moveToCart(Long userId, Long productId);

    /**
     * Move multiple items to cart
     */
    void moveMultipleToCart(Long userId, List<Long> productIds);

    /**
     * Get purchased items history
     */
    List<WishlistItemDto> getPurchasedItems(Long userId);

    /**
     * Get unpurchased items
     */
    List<WishlistItemDto> getUnpurchasedItems(Long userId);

    // ==================== Sharing & Social Features ====================

    /**
     * Share wishlist (generate shareable link)
     */
    WishlistShareDto shareWishlist(Long userId, WishlistShareRequest request);

    /**
     * Get public wishlist by share token
     */
    WishlistSummaryDto getPublicWishlist(String shareToken);

    /**
     * Get user's public wishlist items
     */
    List<WishlistItemDto> getPublicWishlistItems(Long userId);

    /**
     * Update wishlist privacy settings
     */
    void updateWishlistPrivacy(Long userId, boolean isPublic);

    // ==================== Bulk Operations ====================

    /**
     * Add multiple products to wishlist
     */
    List<WishlistItemDto> addMultipleToWishlist(Long userId, List<AddToWishlistRequest> requests);

    /**
     * Remove multiple products from wishlist
     */
    void removeMultipleFromWishlist(Long userId, List<Long> productIds);

    /**
     * Update multiple wishlist items
     */
    void updateMultipleItems(Long userId, Map<Long, UpdateWishlistItemRequest> updates);

    // ==================== Reminders & Notifications ====================

    /**
     * Set reminder for wishlist item
     */
    WishlistItemDto setReminder(Long userId, Long productId, WishlistReminderRequest request);

    /**
     * Get items with due reminders
     */
    List<WishlistItemDto> getItemsWithDueReminders(Long userId);

    /**
     * Cancel reminder
     */
    void cancelReminder(Long userId, Long productId);

    // ==================== Analytics & Insights ====================

    /**
     * Get wishlist analytics
     */
    WishlistAnalyticsDto getWishlistAnalytics(Long userId);

    /**
     * Get price history for wishlist item
     */
    List<PriceHistoryDto> getPriceHistory(Long userId, Long productId);

    /**
     * Get recommendations based on wishlist
     */
    List<ProductRecommendationDto> getWishlistRecommendations(Long userId);

    // ==================== Import/Export ====================

    /**
     * Export wishlist to CSV
     */
    byte[] exportWishlistToCsv(Long userId);

    /**
     * Export wishlist to PDF
     */
    byte[] exportWishlistToPdf(Long userId);

    /**
     * Import wishlist from CSV
     */
    void importWishlistFromCsv(Long userId, byte[] csvData);

    // ==================== Comparison & Shopping ====================

    /**
     * Compare prices across wishlist items
     */
    WishlistPriceComparisonDto compareWishlistPrices(Long userId);

    /**
     * Get total cost of wishlist
     */
    WishlistCostSummaryDto getWishlistCost(Long userId);

    /**
     * Get available items (in stock and within budget)
     */
    List<WishlistItemDto> getAvailableItems(Long userId);

    /**
     * Optimize wishlist by priority and budget
     */
    List<WishlistItemDto> optimizeWishlist(Long userId, WishlistOptimizationRequest request);
}