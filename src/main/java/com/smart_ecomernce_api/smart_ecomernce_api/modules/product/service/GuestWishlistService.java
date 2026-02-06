package com.smart_ecomernce_api.smart_ecomernce_api.modules.product.service;

import com.smart_ecomernce_api.smart_ecomernce_api.modules.product.dto.AddToWishlistRequest;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.product.dto.WishlistItemDto;

import java.util.List;

/**
 * Service for managing guest wishlist operations
 * Handles wishlist functionality for non-authenticated users
 */
public interface GuestWishlistService {

    /**
     * Generate a new guest session ID
     */
    String generateGuestSessionId();

    /**
     * Add product to guest wishlist
     */
    WishlistItemDto addToGuestWishlist(String guestSessionId, AddToWishlistRequest request);

    /**
     * Get guest wishlist items
     */
    List<WishlistItemDto> getGuestWishlist(String guestSessionId);

    /**
     * Remove product from guest wishlist
     */
    void removeFromGuestWishlist(String guestSessionId, Long productId);

    /**
     * Clear entire guest wishlist
     */
    void clearGuestWishlist(String guestSessionId);

    /**
     * Check if product is in guest wishlist
     */
    boolean isInGuestWishlist(String guestSessionId, Long productId);

    /**
     * Merge guest wishlist with user wishlist after login
     */
    void mergeGuestWishlistToUser(String guestSessionId, Long userId);

    /**
     * Clean up expired guest sessions
     */
    void cleanupExpiredGuestSessions();

    /**
     * Get count of items in guest wishlist
     */
    Long getGuestWishlistCount(String guestSessionId);

    /**
     * Transfer guest wishlist to email for later retrieval
     */
    void sendGuestWishlistToEmail(String guestSessionId, String email);
}