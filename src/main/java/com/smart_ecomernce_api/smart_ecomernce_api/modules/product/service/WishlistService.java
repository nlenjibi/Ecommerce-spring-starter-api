package com.smart_ecomernce_api.smart_ecomernce_api.modules.product.service;


import com.smart_ecomernce_api.smart_ecomernce_api.modules.product.dto.AddToWishlistRequest;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.product.dto.UpdateWishlistItemRequest;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.product.dto.WishlistItemDto;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.product.dto.WishlistSummaryDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface WishlistService {

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
     * Get items with price drops
     */
    List<WishlistItemDto> getItemsWithPriceDrops(Long userId);

    /**
     * Mark item as purchased
     */
    WishlistItemDto markAsPurchased(Long userId, Long productId);

    /**
     * Clear entire wishlist
     */
    void clearWishlist(Long userId);

    /**
     * Move wishlist item to cart
     */
    void moveToCart(Long userId, Long productId);
}