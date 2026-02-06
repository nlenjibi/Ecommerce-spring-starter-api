package com.smart_ecomernce_api.smart_ecomernce_api.modules.cart.entity;

import com.smart_ecomernce_api.smart_ecomernce_api.common.base.BaseEntity;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.product.entity.Product;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Enhanced Cart Entity for Modern E-commerce
 * Supports guest carts, user carts, and advanced features
 */
@Entity
@Table(name = "carts", indexes = {
        @Index(name = "idx_cart_user_status", columnList = "user_id, status"),
        @Index(name = "idx_cart_session", columnList = "session_id"),
        @Index(name = "idx_cart_updated", columnList = "updated_at"),
        @Index(name = "idx_cart_share_token", columnList = "share_token")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Cart extends BaseEntity {

    /**
     * Session ID for guest carts (UUID)
     */
    @Column(name = "session_id", length = 100)
    private String sessionId;

    /**
     * Cart status
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private CartStatus status = CartStatus.ACTIVE;

    /**
     * Associated user (null for guest carts)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    /**
     * Applied coupon code
     */
    @Column(name = "coupon_code", length = 50)
    private String couponCode;

    /**
     * Discount amount from coupon
     */
    @Column(name = "discount_amount", precision = 10, scale = 2)
    private BigDecimal discountAmount;

    /**
     * Cart items
     */
    @OneToMany(
            mappedBy = "cart",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    @Builder.Default
    private Set<CartItem> items = new HashSet<>();

    /**
     * Share token for cart sharing feature
     */
    @Column(name = "share_token", unique = true, length = 100)
    private String shareToken;

    /**
     * Share token expiration
     */
    @Column(name = "share_token_expires_at")
    private LocalDateTime shareTokenExpiresAt;

    /**
     * IP address (for fraud detection)
     */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    /**
     * User agent (for analytics)
     */
    @Column(name = "user_agent", length = 500)
    private String userAgent;

    /**
     * Last validation timestamp
     */
    @Column(name = "last_validated_at")
    private LocalDateTime lastValidatedAt;

    /**
     * Notes or special instructions
     */
    @Column(name = "notes", length = 1000)
    private String notes;

    /**
     * Add item to cart or update quantity if already exists
     */
    public CartItem addItem(Product product, int quantity) {
        if (items == null) {
            items = new HashSet<>();
        }

        for (CartItem item : items) {
            if (item.getProduct().getId().equals(product.getId())) {
                item.setQuantity(item.getQuantity() + quantity);
                return item;
            }
        }

        CartItem newItem = CartItem.builder()
                .cart(this)
                .product(product)
                .quantity(quantity)
                .build();
        items.add(newItem);
        return newItem;
    }

    /**
     * Get cart item by product ID
     */
    public CartItem getItem(Long productId) {
        if (items == null) {
            return null;
        }

        return items.stream()
                .filter(i -> i.getProduct().getId().equals(productId))
                .findFirst()
                .orElse(null);
    }

    /**
     * Remove item from cart by product ID
     */
    public void removeItem(Long productId) {
        if (items == null) {
            return;
        }

        items.removeIf(i -> i.getProduct().getId().equals(productId));
    }

    /**
     * Clear all items from cart
     */
    public void clear() {
        if (items == null) {
            items = new HashSet<>();
            return;
        }

        items.clear();
    }

    /**
     * Check if cart is empty
     */
    public boolean isEmpty() {
        return items == null || items.isEmpty();
    }

    /**
     * Get total price of all items (without discount)
     */
    public BigDecimal getTotalPrice() {
        if (items == null || items.isEmpty()) {
            return BigDecimal.ZERO;
        }

        return items.stream()
                .map(item -> {
                    if (item.getProduct() == null || item.getProduct().getPrice() == null) {
                        return BigDecimal.ZERO;
                    }
                    BigDecimal price = item.getProduct().getPrice();
                    return price.multiply(BigDecimal.valueOf(item.getQuantity()));
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Get final price after applying discount
     */
    public BigDecimal getFinalPrice() {
        BigDecimal total = getTotalPrice();

        if (discountAmount != null) {
            total = total.subtract(discountAmount);
        }

        return total.max(BigDecimal.ZERO);
    }

    /**
     * Get total number of items in cart (sum of quantities)
     */
    public int getItemCount() {
        if (items == null || items.isEmpty()) {
            return 0;
        }

        return items.stream()
                .mapToInt(CartItem::getQuantity)
                .sum();
    }

    /**
     * Get number of unique items in cart
     */
    public int getUniqueItemCount() {
        return items == null ? 0 : items.size();
    }

    /**
     * Mark cart as converted to order
     */
    public void markAsConverted() {
        this.status = CartStatus.CONVERTED;
    }

    /**
     * Mark cart as abandoned
     */
    public void markAsAbandoned() {
        this.status = CartStatus.ABANDONED;
    }

    /**
     * Mark cart as expired
     */
    public void markAsExpired() {
        this.status = CartStatus.EXPIRED;
    }

    /**
     * Check if cart is active
     */
    public boolean isActive() {
        return this.status == CartStatus.ACTIVE;
    }

    /**
     * Check if cart belongs to guest
     */
    public boolean isGuestCart() {
        return this.user == null;
    }

    /**
     * Check if cart is shared
     */
    public boolean isShared() {
        return this.shareToken != null &&
                this.shareTokenExpiresAt != null &&
                this.shareTokenExpiresAt.isAfter(LocalDateTime.now());
    }

    /**
     * Generate share token
     */
    public void generateShareToken(int expiryHours) {
        this.shareToken = UUID.randomUUID().toString();
        this.shareTokenExpiresAt = LocalDateTime.now().plusHours(expiryHours);
    }

    /**
     * Invalidate share token
     */
    public void invalidateShareToken() {
        this.shareToken = null;
        this.shareTokenExpiresAt = null;
    }

    /**
     * Update last validated timestamp
     */
    public void markAsValidated() {
        this.lastValidatedAt = LocalDateTime.now();
    }

    /**
     * Check if cart needs revalidation (older than 1 hour)
     */
    public boolean needsRevalidation() {
        if (lastValidatedAt == null) {
            return true;
        }
        return lastValidatedAt.plusHours(1).isBefore(LocalDateTime.now());
    }

    /**
     * Initialize collections and defaults
     */
    @PostLoad
    @PrePersist
    private void initializeDefaults() {
        if (items == null) {
            items = new HashSet<>();
        }
        if (status == null) {
            status = CartStatus.ACTIVE;
        }
        if (sessionId == null && user == null) {
            sessionId = UUID.randomUUID().toString();
        }
    }

    /**
     * Pre-update hook to set updated timestamp
     */
    @PreUpdate
    private void preUpdate() {
        this.setUpdatedAt(LocalDateTime.now());
    }
}