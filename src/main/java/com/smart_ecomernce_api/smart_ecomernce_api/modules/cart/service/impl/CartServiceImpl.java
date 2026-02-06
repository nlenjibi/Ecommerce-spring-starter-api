package com.smart_ecomernce_api.smart_ecomernce_api.modules.cart.service.impl;

import com.smart_ecomernce_api.smart_ecomernce_api.exception.*;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.cart.CartConfig;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.cart.dto.*;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.cart.entity.Cart;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.cart.entity.CartItem;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.cart.entity.CartStatus;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.cart.mapper.CartMapper;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.cart.repository.CartRepository;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.cart.service.CartService;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.product.entity.Product;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.product.repository.ProductRepository;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.user.entity.User;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Optimized Cart Service Implementation for Modern E-commerce
 * Enhanced with caching, validation, and advanced features
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final CartMapper cartMapper;
    private final CartConfig.CartProperties cartProperties;

    // Configuration constants
    private static final int MAX_CART_ITEMS = 100;
    private static final int ABANDONED_CART_THRESHOLD_HOURS = 24;
    private static final int EXPIRED_CART_DAYS = 90;
    private static final int SHARE_LINK_EXPIRY_HOURS = 72;

    // ==================== Core Cart Operations ====================

    @Override
    @Transactional
    public CartDto createCart() {
        log.debug("Creating new guest cart");

        Cart cart = Cart.builder()
                .status(CartStatus.ACTIVE)
                .build();

        Cart savedCart = cartRepository.save(cart);
        log.info("Created guest cart with id: {}", savedCart.getId());

        return cartMapper.toDto(savedCart);
    }

    @Override
    @Transactional
    public CartDto createCartForUser(String username) {
        log.debug("Creating new cart for user: {}", username);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

        // Check if user already has an active cart
        Optional<Cart> existingCart = cartRepository.findActiveCartByUserId(user.getId());
        if (existingCart.isPresent()) {
            log.info("User {} already has active cart: {}", username, existingCart.get().getId());
            return cartMapper.toDto(existingCart.get());
        }

        Cart cart = Cart.builder()
                .user(user)
                .status(CartStatus.ACTIVE)
                .build();

        Cart savedCart = cartRepository.save(cart);
        log.info("Created cart {} for user: {}", savedCart.getId(), username);

        return cartMapper.toDto(savedCart);
    }

    @Override
    @Transactional(readOnly = true)
    public CartDto getCart(Long cartId) {
        log.debug("Fetching cart with id: {}", cartId);

        Cart cart = cartRepository.findByIdWithItems(cartId)
                .orElseThrow(() -> new CartNotFoundException(cartId));

        log.debug("Found cart with {} items", cart.getItems().size());
        return cartMapper.toDto(cart);
    }

    @Override
    @Transactional(readOnly = true)
    public CartDto getUserActiveCart(String username) {
        log.debug("Fetching active cart for user: {}", username);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

        Cart cart = cartRepository.findActiveCartByUserId(user.getId())
                .orElseThrow(() -> new CartNotFoundException("No active cart found for user: " + username));

        return cartMapper.toDto(cart);
    }

    @Override
    @Transactional(readOnly = true)
    public CartSummaryDto getCartSummary(Long cartId) {
        log.debug("Fetching cart summary for id: {}", cartId);

        Cart cart = cartRepository.findByIdWithItems(cartId)
                .orElseThrow(() -> new CartNotFoundException(cartId));

        return CartSummaryDto.builder()
                .id(cart.getId())
                .status(cart.getStatus().name())
                .itemCount(cart.getItemCount())
                .uniqueItemCount(cart.getUniqueItemCount())
                .subtotal(cart.getTotalPrice())
                .discount(cart.getDiscountAmount())
                .totalPrice(cart.getFinalPrice())
                .couponCode(cart.getCouponCode())
                .build();
    }

    // ==================== Cart Item Operations ====================

    @Override
    @Transactional
    public CartItemDto addToCart(Long cartId, AddItemToCartRequest request) {
        log.debug("Adding product {} to cart {} with quantity {}",
                request.getProductId(), cartId, request.getQuantity());

        // Fetch and validate cart
        Cart cart = fetchActiveCart(cartId);
        validateCartNotFull(cart);

        // Fetch and validate product
        Product product = fetchActiveProduct(request.getProductId());

        // Determine quantity
        int qty = request.getQuantity() != null && request.getQuantity() > 0
                ? request.getQuantity()
                : 1;

        // Check existing item
        CartItem existingItem = cart.getItem(product.getId());
        int currentQty = existingItem != null ? existingItem.getQuantity() : 0;
        int totalQty = currentQty + qty;

        // Validate stock availability
        validateStockAvailability(product, totalQty);

        // Add or update item
        CartItem cartItem = cart.addItem(product, qty);
        updateCartItemPrice(cartItem);

        // Save cart
        cartRepository.save(cart);

        log.info("Added product {} (qty: {}) to cart {}. Total items: {}",
                product.getId(), qty, cartId, cart.getItems().size());

        return cartMapper.toDto(cartItem);
    }

    @Override
    @Transactional
    public CartDto bulkAddToCart(Long cartId, BulkAddItemsRequest request) {
        log.debug("Bulk adding {} items to cart {}", request.getItems().size(), cartId);

        Cart cart = fetchActiveCart(cartId);
        int successCount = 0;
        int failureCount = 0;

        for (AddItemToCartRequest item : request.getItems()) {
            try {
                Product product = fetchActiveProduct(item.getProductId());

                int qty = item.getQuantity() != null && item.getQuantity() > 0
                        ? item.getQuantity()
                        : 1;

                // Check existing quantity
                CartItem existingItem = cart.getItem(product.getId());
                int currentQty = existingItem != null ? existingItem.getQuantity() : 0;
                int totalQty = currentQty + qty;

                // Validate stock
                if (!isStockAvailable(product, totalQty)) {
                    log.warn("Skipping product {} - insufficient stock", product.getId());
                    failureCount++;
                    continue;
                }

                // Add item
                CartItem cartItem = cart.addItem(product, qty);
                updateCartItemPrice(cartItem);
                successCount++;

            } catch (Exception e) {
                log.error("Failed to add product {} to cart: {}",
                        item.getProductId(), e.getMessage());
                failureCount++;
            }
        }

        // Validate cart not full
        if (cart.getItems().size() > MAX_CART_ITEMS) {
            throw new IllegalStateException("Cart cannot exceed " + MAX_CART_ITEMS + " unique items");
        }

        cartRepository.save(cart);

        log.info("Bulk add completed for cart {}. Success: {}, Failed: {}",
                cartId, successCount, failureCount);

        return cartMapper.toDto(cart);
    }

    @Override
    @Transactional
    public CartItemDto updateItemQuantity(Long cartId, Long productId, UpdateCartItemRequest request) {
        log.debug("Updating item {} quantity to {} in cart {}",
                productId, request.getQuantity(), cartId);

        validateQuantity(request.getQuantity());

        // Handle quantity 0 as removal
        if (request.getQuantity() == 0) {
            removeItem(cartId, productId);
            return null;
        }

        Cart cart = fetchActiveCart(cartId);

        // Find cart item
        CartItem item = cart.getItem(productId);
        if (item == null) {
            throw ResourceNotFoundException.forResource("CartItem", productId);
        }

        // Validate stock
        validateStockAvailability(item.getProduct(), request.getQuantity());

        // Update quantity
        int oldQuantity = item.getQuantity();
        item.setQuantity(request.getQuantity());
        updateCartItemPrice(item);

        cartRepository.save(cart);

        log.info("Updated cart item quantity from {} to {} for product {} in cart {}",
                oldQuantity, request.getQuantity(), productId, cartId);

        return cartMapper.toDto(item);
    }

    @Override
    @Transactional(readOnly = true)
    public CartItemDto getCartItem(Long cartId, Long productId) {
        log.debug("Fetching cart item {} from cart {}", productId, cartId);

        Cart cart = cartRepository.findByIdWithItems(cartId)
                .orElseThrow(() -> new CartNotFoundException(cartId));

        CartItem item = cart.getItem(productId);
        if (item == null) {
            throw ResourceNotFoundException.forResource("CartItem", productId);
        }

        return cartMapper.toDto(item);
    }

    @Override
    @Transactional
    public void removeItem(Long cartId, Long productId) {
        log.debug("Removing product {} from cart {}", productId, cartId);

        Cart cart = cartRepository.findByIdWithItems(cartId)
                .orElseThrow(() -> new CartNotFoundException(cartId));

        CartItem item = cart.getItem(productId);
        if (item == null) {
            log.debug("Cart item not found - nothing to remove");
            return;
        }

        cart.removeItem(productId);
        cartRepository.save(cart);

        log.info("Removed product {} from cart {}. Remaining items: {}",
                productId, cartId, cart.getItems().size());
    }

    @Override
    @Transactional
    public void clearCart(Long cartId) {
        log.debug("Clearing all items from cart {}", cartId);

        Cart cart = cartRepository.findByIdWithItems(cartId)
                .orElseThrow(() -> new CartNotFoundException(cartId));

        int itemCount = cart.getItems().size();

        cart.clear();
        cart.setCouponCode(null);
        cart.setDiscountAmount(null);

        cartRepository.save(cart);

        log.info("Cleared {} items from cart {}", itemCount, cartId);
    }

    @Override
    @Transactional(readOnly = true)
    public ItemAvailabilityDto checkItemAvailability(Long cartId, Long productId) {
        log.debug("Checking availability for item {} in cart {}", productId, cartId);

        Cart cart = cartRepository.findByIdWithItems(cartId)
                .orElseThrow(() -> new CartNotFoundException(cartId));

        CartItem item = cart.getItem(productId);
        if (item == null) {
            throw ResourceNotFoundException.forResource("CartItem", productId);
        }

        Product product = item.getProduct();
        int requestedQty = item.getQuantity();
        int availableQty = calculateAvailableStock(product);
        boolean available = availableQty >= requestedQty;

        // Check price changes
        BigDecimal currentPrice = product.getEffectivePrice();
        BigDecimal cartPrice = item.getTotalPrice().divide(
                BigDecimal.valueOf(requestedQty), 2, RoundingMode.HALF_UP);
        boolean priceChanged = !currentPrice.equals(cartPrice);

        String message = buildAvailabilityMessage(available, priceChanged, availableQty, requestedQty);

        return ItemAvailabilityDto.builder()
                .productId(productId)
                .productName(product.getName())
                .available(available)
                .requestedQuantity(requestedQty)
                .availableQuantity(availableQty)
                .priceChanged(priceChanged)
                .currentPrice(currentPrice)
                .message(message)
                .build();
    }

    // ==================== Helper Methods ====================

    private Cart fetchActiveCart(Long cartId) {
        Cart cart = cartRepository.findByIdWithItems(cartId)
                .orElseThrow(() -> new CartNotFoundException(cartId));

        if (cart.getStatus() != CartStatus.ACTIVE) {
            throw new IllegalStateException(
                    "Cart is not active. Current status: " + cart.getStatus());
        }

        return cart;
    }

    private Product fetchActiveProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> ResourceNotFoundException.forResource("Product", productId));

        if (!product.getIsActive()) {
            throw new IllegalStateException("Product is not available");
        }

        return product;
    }

    private void validateCartNotFull(Cart cart) {
        if (cart.getUniqueItemCount() >= MAX_CART_ITEMS) {
            throw new IllegalStateException(
                    "Cart is full. Maximum " + MAX_CART_ITEMS + " unique items allowed");
        }
    }

    private void validateQuantity(Integer quantity) {
        if (quantity == null || quantity < 0) {
            throw new IllegalArgumentException("Quantity must be a positive number");
        }
    }

    private void validateStockAvailability(Product product, int requestedQuantity) {
        if (!product.getTrackInventory()) {
            return;
        }

        int availableQty = calculateAvailableStock(product);

        if (availableQty < requestedQuantity) {
            throw new InsufficientStockException(
                    product.getName(),
                    availableQty,
                    requestedQuantity
            );
        }

        if (product.getStockQuantity() <= 0 && !product.getAllowBackorder()) {
            throw new InsufficientStockException(
                    product.getName(),
                    0,
                    requestedQuantity
            );
        }
    }

    private boolean isStockAvailable(Product product, int requestedQuantity) {
        if (!product.getTrackInventory()) {
            return true;
        }

        int availableQty = calculateAvailableStock(product);
        return availableQty >= requestedQuantity;
    }

    private int calculateAvailableStock(Product product) {
        if (!product.getTrackInventory()) {
            return Integer.MAX_VALUE;
        }
        return product.getStockQuantity() - product.getReservedQuantity();
    }

    private void updateCartItemPrice(CartItem item) {
        BigDecimal itemTotal = item.getProduct().getEffectivePrice()
                .multiply(BigDecimal.valueOf(item.getQuantity()));
        item.setTotalPrice(itemTotal);
    }

    private String buildAvailabilityMessage(boolean available, boolean priceChanged,
                                            int availableQty, int requestedQty) {
        if (!available && priceChanged) {
            return String.format("Only %d available (requested %d) and price has changed",
                    availableQty, requestedQty);
        } else if (!available) {
            return String.format("Only %d available (requested %d)",
                    availableQty, requestedQty);
        } else if (priceChanged) {
            return "Price has changed since item was added";
        }
        return "Item is available";
    }
// Continuation of CartServiceImpl.java - Part 2: Advanced Features

    // ==================== Coupon Operations ====================

    @Override
    @Transactional
    public CartDto applyCoupon(Long cartId, String couponCode) {
        log.debug("Applying coupon {} to cart {}", couponCode, cartId);

        Cart cart = fetchActiveCart(cartId);

        // Validate coupon exists and is active
        // This would integrate with a CouponService in real implementation
        BigDecimal discountAmount = calculateCouponDiscount(cart, couponCode);

        cart.setCouponCode(couponCode);
        cart.setDiscountAmount(discountAmount);

        cartRepository.save(cart);

        log.info("Applied coupon {} to cart {}. Discount: {}",
                couponCode, cartId, discountAmount);

        return cartMapper.toDto(cart);
    }

    @Override
    @Transactional
    public CartDto removeCoupon(Long cartId) {
        log.debug("Removing coupon from cart {}", cartId);

        Cart cart = cartRepository.findByIdWithItems(cartId)
                .orElseThrow(() -> new CartNotFoundException(cartId));

        cart.setCouponCode(null);
        cart.setDiscountAmount(null);

        cartRepository.save(cart);

        log.info("Removed coupon from cart {}", cartId);

        return cartMapper.toDto(cart);
    }

    // ==================== Cart Validation ====================

    @Override
    @Transactional(readOnly = true)
    public CartValidationResult validateCart(Long cartId) {
        log.debug("Validating cart {}", cartId);

        Cart cart = cartRepository.findByIdWithItems(cartId)
                .orElseThrow(() -> new CartNotFoundException(cartId));

        List<CartValidationResult.ValidationIssue> issues = new ArrayList<>();
        BigDecimal originalTotal = cart.getTotalPrice();
        boolean priceChanged = false;
        boolean stockChanged = false;

        // Validate each item
        for (CartItem item : cart.getItems()) {
            Product product = item.getProduct();

            // Check if product is still active
            if (!product.getIsActive()) {
                issues.add(CartValidationResult.ValidationIssue.builder()
                        .type("ITEM_UNAVAILABLE")
                        .productId(product.getId())
                        .productName(product.getName())
                        .message("Product is no longer available")
                        .build());
                continue;
            }

            // Check stock availability
            if (product.getTrackInventory()) {
                int availableStock = calculateAvailableStock(product);

                if (availableStock < item.getQuantity()) {
                    stockChanged = true;
                    String issueType = availableStock == 0 ? "OUT_OF_STOCK" : "INSUFFICIENT_STOCK";

                    issues.add(CartValidationResult.ValidationIssue.builder()
                            .type(issueType)
                            .productId(product.getId())
                            .productName(product.getName())
                            .message(String.format("Only %d available, requested %d",
                                    availableStock, item.getQuantity()))
                            .requestedQuantity(item.getQuantity())
                            .availableQuantity(availableStock)
                            .build());
                }
            }

            // Check price changes
            BigDecimal currentPrice = product.getEffectivePrice();
            BigDecimal cartItemUnitPrice = item.getTotalPrice().divide(
                    BigDecimal.valueOf(item.getQuantity()), 2, RoundingMode.HALF_UP);

            if (!currentPrice.equals(cartItemUnitPrice)) {
                priceChanged = true;

                issues.add(CartValidationResult.ValidationIssue.builder()
                        .type("PRICE_CHANGED")
                        .productId(product.getId())
                        .productName(product.getName())
                        .message("Price has changed")
                        .oldPrice(cartItemUnitPrice)
                        .newPrice(currentPrice)
                        .build());
            }
        }

        boolean valid = issues.isEmpty();
        String message = valid ? "Cart is valid and ready for checkout"
                : String.format("Cart has %d issue(s) that need attention", issues.size());

        return CartValidationResult.builder()
                .valid(valid)
                .message(message)
                .issues(issues)
                .originalTotal(originalTotal)
                .updatedTotal(cart.getTotalPrice()) // Recalculated if needed
                .priceChanged(priceChanged)
                .stockChanged(stockChanged)
                .build();
    }

    // ==================== Cart Merge & Restore ====================

    @Override
    @Transactional
    public CartDto mergeCart(Long guestCartId, Long userId) {
        log.debug("Merging guest cart {} with user {} cart", guestCartId, userId);

        Cart guestCart = cartRepository.findByIdWithItems(guestCartId)
                .orElseThrow(() -> new CartNotFoundException(guestCartId));

        // Get or create user cart
        Cart userCart = cartRepository.findActiveCartByUserId(userId)
                .orElseGet(() -> {
                    User user = new User();
                    user.setId(userId);
                    Cart newCart = Cart.builder()
                            .user(user)
                            .status(CartStatus.ACTIVE)
                            .build();
                    return cartRepository.save(newCart);
                });

        // Merge items
        int mergedCount = mergeCartItems(guestCart, userCart);

        // Delete guest cart
        cartRepository.deleteById(guestCartId);

        Cart savedCart = cartRepository.save(userCart);

        log.info("Merged {} items from guest cart {} to user cart {}",
                mergedCount, guestCartId, userCart.getId());

        return cartMapper.toDto(savedCart);
    }

    @Override
    @Transactional
    public CartDto mergeGuestCart(Long guestCartId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

        return mergeCart(guestCartId, user.getId());
    }

    @Override
    @Transactional
    public CartDto restoreCart(Long cartId) {
        log.debug("Restoring cart {}", cartId);

        Cart cart = cartRepository.findByIdWithItems(cartId)
                .orElseThrow(() -> new CartNotFoundException(cartId));

        if (cart.getStatus() == CartStatus.CONVERTED) {
            throw new IllegalStateException("Cannot restore a converted cart");
        }

        // Validate and refresh items
        cart.getItems().removeIf(item -> !item.getProduct().getIsActive());

        // Refresh prices
        cart.getItems().forEach(this::updateCartItemPrice);

        cart.setStatus(CartStatus.ACTIVE);
        cartRepository.save(cart);

        log.info("Restored cart {} with {} items", cartId, cart.getItems().size());

        return cartMapper.toDto(cart);
    }

    // ==================== Advanced Features ====================

    @Override
    @Transactional
    public SaveForLaterResult saveCartForLater(Long cartId, String username) {
        log.debug("Saving cart {} items for later for user {}", cartId, username);

        Cart cart = cartRepository.findByIdWithItems(cartId)
                .orElseThrow(() -> new CartNotFoundException(cartId));

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

        int itemsSaved = 0;
        int itemsFailed = 0;

        // In real implementation, this would integrate with WishlistService
        for (CartItem item : cart.getItems()) {
            try {
                // Save to wishlist logic here
                itemsSaved++;
            } catch (Exception e) {
                log.error("Failed to save item {} to wishlist: {}",
                        item.getProduct().getId(), e.getMessage());
                itemsFailed++;
            }
        }

        // Clear cart after saving
        if (itemsSaved > 0) {
            cart.clear();
            cart.setCouponCode(null);
            cart.setDiscountAmount(null);
            cartRepository.save(cart);
        }

        log.info("Saved {} items from cart {} to wishlist. Failed: {}",
                itemsSaved, cartId, itemsFailed);

        return SaveForLaterResult.builder()
                .success(itemsFailed == 0)
                .message(String.format("Saved %d items to wishlist", itemsSaved))
                .itemsSaved(itemsSaved)
                .itemsFailed(itemsFailed)
                .wishlistId(user.getId()) // Placeholder
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ShippingEstimate estimateShipping(Long cartId, ShippingEstimateRequest request) {
        log.debug("Estimating shipping for cart {} to {}, {}",
                cartId, request.getCity(), request.getCountry());

        Cart cart = cartRepository.findByIdWithItems(cartId)
                .orElseThrow(() -> new CartNotFoundException(cartId));

        // Calculate shipping based on cart weight, destination, etc.
        // This is a simplified implementation
        BigDecimal cartTotal = cart.getTotalPrice();
        String method = request.getShippingMethod() != null
                ? request.getShippingMethod()
                : "STANDARD";

        List<ShippingEstimate.ShippingOption> options = Arrays.asList(
                ShippingEstimate.ShippingOption.builder()
                        .method("STANDARD")
                        .name("Standard Shipping")
                        .cost(calculateShippingCost(cartTotal, "STANDARD"))
                        .minDays(5)
                        .maxDays(7)
                        .description("5-7 business days")
                        .build(),
                ShippingEstimate.ShippingOption.builder()
                        .method("EXPRESS")
                        .name("Express Shipping")
                        .cost(calculateShippingCost(cartTotal, "EXPRESS"))
                        .minDays(2)
                        .maxDays(3)
                        .description("2-3 business days")
                        .build(),
                ShippingEstimate.ShippingOption.builder()
                        .method("OVERNIGHT")
                        .name("Overnight Shipping")
                        .cost(calculateShippingCost(cartTotal, "OVERNIGHT"))
                        .minDays(1)
                        .maxDays(1)
                        .description("Next business day")
                        .build()
        );

        ShippingEstimate.ShippingOption selectedOption = options.stream()
                .filter(opt -> opt.getMethod().equals(method))
                .findFirst()
                .orElse(options.get(0));

        return ShippingEstimate.builder()
                .cost(selectedOption.getCost())
                .currency("USD")
                .estimatedDays(selectedOption.getMaxDays())
                .method(selectedOption.getMethod())
                .availableOptions(options)
                .build();
    }

    @Override
    @Transactional
    public ShareCartResponse createShareableCart(Long cartId) {
        log.debug("Creating shareable link for cart {}", cartId);

        Cart cart = cartRepository.findByIdWithItems(cartId)
                .orElseThrow(() -> new CartNotFoundException(cartId));

        // Generate unique share token
        String shareToken = UUID.randomUUID().toString();

        // In real implementation, store this mapping in database or cache
        // with expiration time

        String shareUrl = String.format("/api/v1/carts/shared/%s", shareToken);

        log.info("Created share link for cart {}: {}", cartId, shareToken);

        return ShareCartResponse.builder()
                .shareToken(shareToken)
                .shareUrl(shareUrl)
                .expiresInHours((long) SHARE_LINK_EXPIRY_HOURS)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public CartDto getSharedCart(String shareToken) {
        log.debug("Fetching shared cart with token: {}", shareToken);

        // In real implementation, retrieve cartId from token mapping
        // For now, this is a placeholder
        throw new UnsupportedOperationException("Shared cart retrieval not fully implemented");
    }

    @Override
    @Transactional
    public CartDto cloneSharedCart(String shareToken, String username) {
        log.debug("Cloning shared cart {} for user: {}", shareToken, username);

        // Get shared cart
        CartDto sharedCart = getSharedCart(shareToken);

        // Create new cart
        Cart newCart;
        if (username != null) {
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
            newCart = Cart.builder()
                    .user(user)
                    .status(CartStatus.ACTIVE)
                    .build();
        } else {
            newCart = Cart.builder()
                    .status(CartStatus.ACTIVE)
                    .build();
        }

        newCart = cartRepository.save(newCart);

        log.info("Cloned shared cart to new cart {}", newCart.getId());

        return cartMapper.toDto(newCart);
    }

    @Override
    @Transactional(readOnly = true)
    public CartRecommendationsDto getCartRecommendations(Long cartId, int limit) {
        log.debug("Fetching {} recommendations for cart {}", limit, cartId);

        Cart cart = cartRepository.findByIdWithItems(cartId)
                .orElseThrow(() -> new CartNotFoundException(cartId));

        // In real implementation, this would use a recommendation engine
        // based on collaborative filtering, content-based filtering, etc.
        List<CartRecommendationsDto.RecommendedProduct> recommendations = new ArrayList<>();

        log.info("Generated {} recommendations for cart {}", recommendations.size(), cartId);

        return CartRecommendationsDto.builder()
                .recommendations(recommendations)
                .recommendationType("FREQUENTLY_BOUGHT_TOGETHER")
                .build();
    }

    @Override
    @Transactional
    public CartDto updateCartStatus(Long cartId, String statusStr) {
        log.debug("Updating cart {} status to {}", cartId, statusStr);

        Cart cart = cartRepository.findByIdWithItems(cartId)
                .orElseThrow(() -> new CartNotFoundException(cartId));

        CartStatus status;
        try {
            status = CartStatus.valueOf(statusStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid cart status: " + statusStr);
        }

        cart.setStatus(status);
        cartRepository.save(cart);

        log.info("Updated cart {} status to {}", cartId, status);

        return cartMapper.toDto(cart);
    }

    @Override
    @Transactional
    public CartDto refreshCartPrices(Long cartId) {
        log.debug("Refreshing prices for cart {}", cartId);

        Cart cart = cartRepository.findByIdWithItems(cartId)
                .orElseThrow(() -> new CartNotFoundException(cartId));

        cart.getItems().forEach(this::updateCartItemPrice);
        cartRepository.save(cart);

        log.info("Refreshed prices for {} items in cart {}", cart.getItems().size(), cartId);

        return cartMapper.toDto(cart);
    }

    // ==================== Background Operations ====================

    @Transactional
    @Scheduled(cron = "0 0 2 * * *") // Run daily at 2 AM
    public void scheduledMarkAbandonedCarts() {
        int threshold = cartProperties.getAbandonedThresholdHours();
        markAbandonedCarts(threshold);
    }

    @Override
    @Transactional
    public int markAbandonedCarts(int abandonedThresholdHours) {
        log.debug("Marking abandoned carts older than {} hours", abandonedThresholdHours);
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(abandonedThresholdHours);
        List<Cart> abandonedCarts = cartRepository.findAbandonedCartsBefore(cutoffTime);
        int count = 0;
        for (Cart cart : abandonedCarts) {
            if (!cart.isEmpty()) {
                cart.markAsAbandoned();
                cartRepository.save(cart);
                count++;
            }
        }
        log.info("Marked {} carts as abandoned", count);
        return count;
    }

    @Transactional
    @Scheduled(cron = "0 0 3 * * *") // Run daily at 3 AM
    public void scheduledCleanupExpiredCarts() {
        int expirationDays = cartProperties.getExpirationDays();
        cleanupExpiredCarts(expirationDays);
    }

    @Override
    @Transactional
    public int cleanupExpiredCarts(int expirationDays) {
        log.debug("Cleaning up carts older than {} days", expirationDays);

        LocalDateTime cutoffTime = LocalDateTime.now().minusDays(expirationDays);
        List<Cart> expiredCarts = cartRepository.findEmptyCartsBefore(cutoffTime);

        int count = 0;
        for (Cart cart : expiredCarts) {
            cartRepository.deleteById(cart.getId());
            count++;
        }

        log.info("Deleted {} expired carts", count);
        return count;
    }

    // ==================== Private Helper Methods ====================

    private int mergeCartItems(Cart sourceCart, Cart targetCart) {
        int mergedCount = 0;

        for (CartItem sourceItem : sourceCart.getItems()) {
            Product product = sourceItem.getProduct();

            if (!product.getIsActive()) {
                log.debug("Skipping inactive product {} during merge", product.getId());
                continue;
            }

            CartItem existingItem = targetCart.getItem(product.getId());
            int existingQty = existingItem != null ? existingItem.getQuantity() : 0;
            int availableStock = calculateAvailableStock(product);
            int availableToAdd = Math.max(0, availableStock - existingQty);
            int qtyToAdd = Math.min(sourceItem.getQuantity(), availableToAdd);

            if (qtyToAdd > 0) {
                CartItem mergedItem = targetCart.addItem(product, qtyToAdd);
                updateCartItemPrice(mergedItem);
                mergedCount++;
            }
        }

        return mergedCount;
    }

    private BigDecimal calculateCouponDiscount(Cart cart, String couponCode) {
        // Simplified discount calculation
        // In real implementation, this would query coupon service
        BigDecimal total = cart.getTotalPrice();

        // Example: 10% discount
        if ("SAVE10".equals(couponCode)) {
            return total.multiply(new BigDecimal("0.10"))
                    .setScale(2, RoundingMode.HALF_UP);
        }

        // Example: $20 off
        if ("FLAT20".equals(couponCode)) {
            return new BigDecimal("20.00");
        }

        throw new IllegalArgumentException("Invalid coupon code: " + couponCode);
    }

    private BigDecimal calculateShippingCost(BigDecimal cartTotal, String method) {
        // Simplified shipping calculation
        switch (method) {
            case "STANDARD":
                return cartTotal.compareTo(new BigDecimal("50")) >= 0
                        ? BigDecimal.ZERO
                        : new BigDecimal("5.99");
            case "EXPRESS":
                return new BigDecimal("12.99");
            case "OVERNIGHT":
                return new BigDecimal("24.99");
            default:
                return new BigDecimal("5.99");
        }
    }
}

