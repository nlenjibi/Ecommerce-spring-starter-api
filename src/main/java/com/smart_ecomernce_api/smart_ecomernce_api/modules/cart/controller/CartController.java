package com.smart_ecomernce_api.smart_ecomernce_api.modules.cart.controller;

import com.smart_ecomernce_api.smart_ecomernce_api.common.response.ApiResponse;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.cart.dto.*;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.cart.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Optimized Cart REST Controller for Modern E-commerce
 * Provides comprehensive endpoints for shopping cart management
 */
@RestController
@RequestMapping("v1/carts")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Cart", description = "Modern shopping cart management API")
public class CartController {

    private final CartService cartService;

    /**
     * Create a new cart (guest or authenticated user)
     */
    @PostMapping
    @Operation(summary = "Create cart", description = "Creates a new empty cart for guest or authenticated user")
    public ResponseEntity<ApiResponse<CartDto>> createCart(
            @RequestParam(value = "username", required = false) String username
    ) {
        log.debug("POST /carts - Creating new cart for user: {}", username != null ? username : "guest");

        CartDto cartDto = username != null
                ? cartService.createCartForUser(username)
                : cartService.createCart();

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Cart created successfully", cartDto));
    }

    /**
     * Get cart by ID with full details
     */
    @GetMapping("/{cartId}")
    @Operation(summary = "Get cart", description = "Retrieves cart by ID with all items and pricing details")
    public ResponseEntity<ApiResponse<CartDto>> getCart(
            @Parameter(description = "Cart ID") @PathVariable Long cartId
    ) {
        log.debug("GET /carts/{} - Fetching cart", cartId);
        CartDto cartDto = cartService.getCart(cartId);
        return ResponseEntity.ok(ApiResponse.success("Cart fetched successfully", cartDto));
    }

    /**
     * Get current user's active cart
     */
    @GetMapping("/me")
    @Operation(summary = "Get my cart", description = "Retrieves the authenticated user's active cart")
    public ResponseEntity<ApiResponse<CartDto>> getMyCart(
            @RequestParam("username") String username
    ) {
        log.debug("GET /carts/me - Fetching cart for user: {}", username);
        CartDto cartDto = cartService.getUserActiveCart(username);
        return ResponseEntity.ok(ApiResponse.success("User cart fetched successfully", cartDto));
    }

    /**
     * Add item to cart
     */
    @PostMapping("/{cartId}/items")
    @Operation(summary = "Add item to cart", description = "Adds a product to the cart or updates quantity if exists")
    public ResponseEntity<ApiResponse<CartItemDto>> addItemToCart(
            @Parameter(description = "Cart ID") @PathVariable Long cartId,
            @Valid @RequestBody AddItemToCartRequest request
    ) {
        log.debug("POST /carts/{}/items - Adding product {}", cartId, request.getProductId());
        CartItemDto cartItemDto = cartService.addToCart(cartId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Item added to cart", cartItemDto));
    }

    /**
     * Bulk add items to cart
     */
    @PostMapping("/{cartId}/items/bulk")
    @Operation(summary = "Bulk add items", description = "Adds multiple products to cart in a single request")
    public ResponseEntity<ApiResponse<CartDto>> bulkAddItemsToCart(
            @Parameter(description = "Cart ID") @PathVariable Long cartId,
            @Valid @RequestBody BulkAddItemsRequest request
    ) {
        log.debug("POST /carts/{}/items/bulk - Adding {} items", cartId, request.getItems().size());
        CartDto cartDto = cartService.bulkAddToCart(cartId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Bulk items added to cart", cartDto));
    }

    /**
     * Update cart item quantity
     */
    @PutMapping("/{cartId}/items/{productId}")
    @Operation(summary = "Update item quantity", description = "Updates the quantity of a specific cart item")
    public ResponseEntity<ApiResponse<CartItemDto>> updateCartItem(
            @Parameter(description = "Cart ID") @PathVariable Long cartId,
            @Parameter(description = "Product ID") @PathVariable Long productId,
            @Valid @RequestBody UpdateCartItemRequest request
    ) {
        log.debug("PUT /carts/{}/items/{} - Updating quantity to {}",
                cartId, productId, request.getQuantity());
        CartItemDto cartItemDto = cartService.updateItemQuantity(cartId, productId, request);
        return ResponseEntity.ok(ApiResponse.success("Cart item quantity updated", cartItemDto));
    }

    /**
     * Remove item from cart
     */
    @DeleteMapping("/{cartId}/items/{productId}")
    @Operation(summary = "Remove item", description = "Removes a specific item from the cart")
    public ResponseEntity<ApiResponse<Void>> removeItemFromCart(
            @Parameter(description = "Cart ID") @PathVariable Long cartId,
            @Parameter(description = "Product ID") @PathVariable Long productId
    ) {
        log.debug("DELETE /carts/{}/items/{} - Removing item", cartId, productId);
        cartService.removeItem(cartId, productId);
        return ResponseEntity.ok(ApiResponse.success("Item removed from cart", null));
    }

    /**
     * Clear all items from cart
     */
    @DeleteMapping("/{cartId}/items")
    @Operation(summary = "Clear cart", description = "Removes all items from the cart while preserving the cart")
    public ResponseEntity<ApiResponse<Void>> clearCart(
            @Parameter(description = "Cart ID") @PathVariable Long cartId
    ) {
        log.debug("DELETE /carts/{}/items - Clearing cart", cartId);
        cartService.clearCart(cartId);
        return ResponseEntity.ok(ApiResponse.success("Cart cleared", null));
    }

    /**
     * Apply coupon to cart
     */
    @PostMapping("/{cartId}/coupons")
    @Operation(summary = "Apply coupon", description = "Applies a discount coupon to the cart")
    public ResponseEntity<ApiResponse<CartDto>> applyCoupon(
            @Parameter(description = "Cart ID") @PathVariable Long cartId,
            @Valid @RequestBody ApplyCouponRequest request
    ) {
        log.debug("POST /carts/{}/coupons - Applying coupon: {}", cartId, request.getCouponCode());
        CartDto cartDto = cartService.applyCoupon(cartId, request.getCouponCode());
        return ResponseEntity.ok(ApiResponse.success("Coupon applied", cartDto));
    }

    /**
     * Remove coupon from cart
     */
    @DeleteMapping("/{cartId}/coupons")
    @Operation(summary = "Remove coupon", description = "Removes the applied coupon from the cart")
    public ResponseEntity<ApiResponse<CartDto>> removeCoupon(
            @Parameter(description = "Cart ID") @PathVariable Long cartId
    ) {
        log.debug("DELETE /carts/{}/coupons - Removing coupon", cartId);
        CartDto cartDto = cartService.removeCoupon(cartId);
        return ResponseEntity.ok(ApiResponse.success("Coupon removed", cartDto));
    }

    /**
     * Validate cart before checkout
     */
    @PostMapping("/{cartId}/validate")
    @Operation(summary = "Validate cart", description = "Validates cart items for stock availability and pricing before checkout")
    public ResponseEntity<ApiResponse<CartValidationResult>> validateCart(
            @Parameter(description = "Cart ID") @PathVariable Long cartId
    ) {
        log.debug("POST /carts/{}/validate - Validating cart", cartId);
        CartValidationResult result = cartService.validateCart(cartId);
        return ResponseEntity.ok(ApiResponse.success("Cart validated", result));
    }

    /**
     * Get cart summary (lightweight version)
     */
    @GetMapping("/{cartId}/summary")
    @Operation(summary = "Get cart summary", description = "Retrieves cart summary with item counts and total price")
    public ResponseEntity<ApiResponse<CartSummaryDto>> getCartSummary(
            @Parameter(description = "Cart ID") @PathVariable Long cartId
    ) {
        log.debug("GET /carts/{}/summary - Fetching cart summary", cartId);
        CartSummaryDto summary = cartService.getCartSummary(cartId);
        return ResponseEntity.ok(ApiResponse.success("Cart summary fetched", summary));
    }

    /**
     * Merge guest cart with user cart after login
     */
    @PostMapping("/merge")
    @Operation(summary = "Merge carts", description = "Merges a guest cart into user's cart after authentication")
    public ResponseEntity<ApiResponse<CartDto>> mergeCarts(
            @Valid @RequestBody MergeCartRequest request,
            @RequestParam("username") String username
    ) {
        log.debug("POST /carts/merge - Merging guest cart {} to user cart", request.getGuestCartId());
        CartDto cartDto = cartService.mergeGuestCart(
                request.getGuestCartId(),
                username
        );
        return ResponseEntity.ok(ApiResponse.success("Carts merged", cartDto));
    }

    /**
     * Restore abandoned cart items
     */
    @PostMapping("/{cartId}/restore")
    @Operation(summary = "Restore cart", description = "Restores an abandoned or expired cart to active status")
    public ResponseEntity<ApiResponse<CartDto>> restoreCart(
            @Parameter(description = "Cart ID") @PathVariable Long cartId
    ) {
        log.debug("POST /carts/{}/restore - Restoring cart", cartId);
        CartDto cartDto = cartService.restoreCart(cartId);
        return ResponseEntity.ok(ApiResponse.success("Cart restored", cartDto));
    }

    /**
     * Get cart item details
     */
    @GetMapping("/{cartId}/items/{productId}")
    @Operation(summary = "Get cart item", description = "Retrieves details of a specific cart item")
    public ResponseEntity<ApiResponse<CartItemDto>> getCartItem(
            @Parameter(description = "Cart ID") @PathVariable Long cartId,
            @Parameter(description = "Product ID") @PathVariable Long productId
    ) {
        log.debug("GET /carts/{}/items/{} - Fetching cart item", cartId, productId);
        CartItemDto item = cartService.getCartItem(cartId, productId);
        return ResponseEntity.ok(ApiResponse.success("Cart item fetched", item));
    }

    /**
     * Check cart item availability
     */
    @GetMapping("/{cartId}/items/{productId}/availability")
    @Operation(summary = "Check item availability", description = "Checks if cart item is still available in stock")
    public ResponseEntity<ApiResponse<ItemAvailabilityDto>> checkItemAvailability(
            @Parameter(description = "Cart ID") @PathVariable Long cartId,
            @Parameter(description = "Product ID") @PathVariable Long productId
    ) {
        log.debug("GET /carts/{}/items/{}/availability - Checking availability", cartId, productId);
        ItemAvailabilityDto availability = cartService.checkItemAvailability(cartId, productId);
        return ResponseEntity.ok(ApiResponse.success("Item availability checked", availability));
    }

    /**
     * Save cart for later (convert to wishlist)
     */
    @PostMapping("/{cartId}/save-for-later")
    @Operation(summary = "Save for later", description = "Saves cart items to wishlist and clears the cart")
    public ResponseEntity<ApiResponse<SaveForLaterResult>> saveForLater(
            @Parameter(description = "Cart ID") @PathVariable Long cartId,
            @RequestParam("username") String username
    ) {
        log.debug("POST /carts/{}/save-for-later - Saving cart items", cartId);
        SaveForLaterResult result = cartService.saveCartForLater(cartId, username);
        return ResponseEntity.ok(ApiResponse.success("Cart saved for later", result));
    }

    /**
     * Estimate shipping for cart
     */
    @PostMapping("/{cartId}/estimate-shipping")
    @Operation(summary = "Estimate shipping", description = "Calculates shipping cost estimate for cart")
    public ResponseEntity<ApiResponse<ShippingEstimate>> estimateShipping(
            @Parameter(description = "Cart ID") @PathVariable Long cartId,
            @Valid @RequestBody ShippingEstimateRequest request
    ) {
        log.debug("POST /carts/{}/estimate-shipping - Estimating shipping", cartId);
        ShippingEstimate estimate = cartService.estimateShipping(cartId, request);
        return ResponseEntity.ok(ApiResponse.success("Shipping estimated", estimate));
    }

    /**
     * Share cart with others
     */
    @PostMapping("/{cartId}/share")
    @Operation(summary = "Share cart", description = "Generates a shareable link for the cart")
    public ResponseEntity<ApiResponse<ShareCartResponse>> shareCart(
            @Parameter(description = "Cart ID") @PathVariable Long cartId
    ) {
        log.debug("POST /carts/{}/share - Creating share link", cartId);
        ShareCartResponse response = cartService.createShareableCart(cartId);
        return ResponseEntity.ok(ApiResponse.success("Cart share link created", response));
    }

    /**
     * Get shared cart
     */
    @GetMapping("/shared/{shareToken}")
    @Operation(summary = "Get shared cart", description = "Retrieves cart details from a share token")
    public ResponseEntity<ApiResponse<CartDto>> getSharedCart(
            @Parameter(description = "Share token") @PathVariable String shareToken
    ) {
        log.debug("GET /carts/shared/{} - Fetching shared cart", shareToken);
        CartDto cartDto = cartService.getSharedCart(shareToken);
        return ResponseEntity.ok(ApiResponse.success("Shared cart fetched", cartDto));
    }

    /**
     * Clone shared cart to new cart
     */
    @PostMapping("/shared/{shareToken}/clone")
    @Operation(summary = "Clone shared cart", description = "Creates a new cart from a shared cart")
    public ResponseEntity<ApiResponse<CartDto>> cloneSharedCart(
            @Parameter(description = "Share token") @PathVariable String shareToken,
            @RequestParam(value = "username", required = false) String username
    ) {
        log.debug("POST /carts/shared/{}/clone - Cloning shared cart", shareToken);
        CartDto cartDto = cartService.cloneSharedCart(shareToken, username);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Shared cart cloned", cartDto));
    }

    /**
     * Get cart recommendations
     */
    @GetMapping("/{cartId}/recommendations")
    @Operation(summary = "Get recommendations", description = "Retrieves product recommendations based on cart items")
    public ResponseEntity<ApiResponse<CartRecommendationsDto>> getRecommendations(
            @Parameter(description = "Cart ID") @PathVariable Long cartId,
            @RequestParam(defaultValue = "10") int limit
    ) {
        log.debug("GET /carts/{}/recommendations - Fetching {} recommendations", cartId, limit);
        CartRecommendationsDto recommendations = cartService.getCartRecommendations(cartId, limit);
        return ResponseEntity.ok(ApiResponse.success("Cart recommendations fetched", recommendations));
    }

    /**
     * Update cart status (admin/system endpoint)
     */
    @PatchMapping("/{cartId}/status")
    @Operation(summary = "Update cart status", description = "Updates cart status (admin operation)")
    public ResponseEntity<ApiResponse<CartDto>> updateCartStatus(
            @Parameter(description = "Cart ID") @PathVariable Long cartId,
            @Valid @RequestBody UpdateCartStatusRequest request
    ) {
        log.debug("PATCH /carts/{}/status - Updating status to {}", cartId, request.getStatus());
        CartDto cartDto = cartService.updateCartStatus(cartId, request.getStatus());
        return ResponseEntity.ok(ApiResponse.success("Cart status updated", cartDto));
    }
}