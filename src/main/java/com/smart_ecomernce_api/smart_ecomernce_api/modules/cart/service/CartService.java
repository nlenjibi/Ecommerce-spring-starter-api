package com.smart_ecomernce_api.smart_ecomernce_api.modules.cart.service;

import com.smart_ecomernce_api.smart_ecomernce_api.modules.cart.dto.*;

/**
 * Optimized Cart Service Interface for Modern E-commerce
 * Provides comprehensive cart management operations
 */
public interface CartService {

    // ==================== Core Cart Operations ====================

    /**
     * Create a new guest cart
     */
    CartDto createCart();

    /**
     * Create a new cart for authenticated user
     */
    CartDto createCartForUser(String username);

    /**
     * Get cart by ID with all items
     */
    CartDto getCart(Long cartId);

    /**
     * Get user's active cart
     */
    CartDto getUserActiveCart(String username);

    /**
     * Get lightweight cart summary
     */
    CartSummaryDto getCartSummary(Long cartId);

    // ==================== Cart Item Operations ====================

    /**
     * Add single item to cart
     */
    CartItemDto addToCart(Long cartId, AddItemToCartRequest request);

    /**
     * Add multiple items to cart in bulk
     */
    CartDto bulkAddToCart(Long cartId, BulkAddItemsRequest request);

    /**
     * Update cart item quantity
     */
    CartItemDto updateItemQuantity(Long cartId, Long productId, UpdateCartItemRequest request);

    /**
     * Get specific cart item details
     */
    CartItemDto getCartItem(Long cartId, Long productId);

    /**
     * Remove item from cart
     */
    void removeItem(Long cartId, Long productId);

    /**
     * Clear all items from cart
     */
    void clearCart(Long cartId);

    /**
     * Check item availability in cart
     */
    ItemAvailabilityDto checkItemAvailability(Long cartId, Long productId);

    // ==================== Coupon Operations ====================

    /**
     * Apply coupon code to cart
     */
    CartDto applyCoupon(Long cartId, String couponCode);

    /**
     * Remove coupon from cart
     */
    CartDto removeCoupon(Long cartId);

    // ==================== Cart Validation ====================

    /**
     * Validate cart before checkout
     * Checks stock availability, pricing, and item validity
     */
    CartValidationResult validateCart(Long cartId);

    // ==================== Cart Merge & Restore ====================

    /**
     * Merge guest cart into user cart
     */
    CartDto mergeCart(Long guestCartId, Long userId);

    /**
     * Merge guest cart into user cart by username
     */
    CartDto mergeGuestCart(Long guestCartId, String username);

    /**
     * Restore abandoned or expired cart
     */
    CartDto restoreCart(Long cartId);

    // ==================== Advanced Features ====================

    /**
     * Save cart items to wishlist
     */
    SaveForLaterResult saveCartForLater(Long cartId, String username);

    /**
     * Estimate shipping cost for cart
     */
    ShippingEstimate estimateShipping(Long cartId, ShippingEstimateRequest request);

    /**
     * Create shareable cart link
     */
    ShareCartResponse createShareableCart(Long cartId);

    /**
     * Get cart from share token
     */
    CartDto getSharedCart(String shareToken);

    /**
     * Clone shared cart to new cart
     */
    CartDto cloneSharedCart(String shareToken, String username);

    /**
     * Get product recommendations based on cart
     */
    CartRecommendationsDto getCartRecommendations(Long cartId, int limit);

    /**
     * Update cart status
     */
    CartDto updateCartStatus(Long cartId, String status);

    // ==================== Background Operations ====================

    /**
     * Mark abandoned carts (scheduled task)
     */
    int markAbandonedCarts(int abandonedThresholdHours);

    /**
     * Clean up expired carts (scheduled task)
     */
    int cleanupExpiredCarts(int expirationDays);

    /**
     * Refresh cart item prices
     */
    CartDto refreshCartPrices(Long cartId);
}