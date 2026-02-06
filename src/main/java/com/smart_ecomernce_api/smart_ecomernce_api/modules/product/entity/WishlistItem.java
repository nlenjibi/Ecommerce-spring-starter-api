package com.smart_ecomernce_api.smart_ecomernce_api.modules.product.entity;

import com.smart_ecomernce_api.smart_ecomernce_api.common.base.BaseEntity;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.user.entity.User;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Enhanced Wishlist Item Entity with Guest Support
 * Supports both authenticated users and guest sessions
 */
@Entity
@Table(
        name = "wishlist_items",
        indexes = {
                @Index(name = "idx_wishlist_user", columnList = "user_id"),
                @Index(name = "idx_wishlist_guest_session", columnList = "guest_session_id"),
                @Index(name = "idx_wishlist_product", columnList = "product_id"),
                @Index(name = "idx_wishlist_created", columnList = "created_at"),
                @Index(name = "idx_wishlist_priority", columnList = "priority"),
                @Index(name = "idx_wishlist_purchased", columnList = "purchased")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_product", columnNames = {"user_id", "product_id"}),
                @UniqueConstraint(name = "uk_guest_product", columnNames = {"guest_session_id", "product_id"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WishlistItem extends BaseEntity {

    /**
     * Authenticated user (nullable for guest users)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = true)
    private User user;

    /**
     * Guest session identifier (for non-authenticated users)
     * UUID format recommended
     */
    @Column(name = "guest_session_id", length = 100)
    private String guestSessionId;

    /**
     * Product in the wishlist
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    /**
     * User's personal notes about this product
     */
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    /**
     * Priority level (for gift lists, shopping planning)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "priority", length = 20)
    @Builder.Default
    private WishlistPriority priority = WishlistPriority.MEDIUM;

    /**
     * Desired quantity
     */
    @Column(name = "desired_quantity")
    @Builder.Default
    private Integer desiredQuantity = 1;

    /**
     * Price when added to wishlist (track price changes)
     */
    @Column(name = "price_when_added", precision = 10, scale = 2)
    private BigDecimal priceWhenAdded;

    /**
     * Enable price drop notifications
     */
    @Column(name = "notify_on_price_drop")
    @Builder.Default
    private Boolean notifyOnPriceDrop = false;

    /**
     * Target price for notification
     */
    @Column(name = "target_price", precision = 10, scale = 2)
    private BigDecimal targetPrice;

    /**
     * Enable stock availability notifications
     */
    @Column(name = "notify_on_stock")
    @Builder.Default
    private Boolean notifyOnStock = false;

    /**
     * Whether product was purchased from this wishlist
     */
    @Column(name = "purchased")
    @Builder.Default
    private Boolean purchased = false;

    /**
     * When product was purchased
     */
    @Column(name = "purchased_at")
    private LocalDateTime purchasedAt;

    /**
     * Is this item public (for shared wishlists)
     */
    @Column(name = "is_public")
    @Builder.Default
    private Boolean isPublic = false;

    /**
     * Collection/list name (e.g., "Christmas Gifts", "Home Office")
     */
    @Column(name = "collection_name", length = 100)
    private String collectionName;

    /**
     * Tags for better organization
     */
    @Column(name = "tags", length = 500)
    private String tags;

    /**
     * Email for guest notification (if guest wants notifications)
     */
    @Column(name = "guest_email", length = 255)
    private String guestEmail;

    /**
     * Last price check timestamp
     */
    @Column(name = "last_price_check")
    private LocalDateTime lastPriceCheck;

    /**
     * Number of times price has dropped
     */
    @Column(name = "price_drop_count")
    @Builder.Default
    private Integer priceDropCount = 0;

    /**
     * Whether user wants to be reminded about this item
     */
    @Column(name = "reminder_enabled")
    @Builder.Default
    private Boolean reminderEnabled = false;

    /**
     * Reminder date/time
     */
    @Column(name = "reminder_date")
    private LocalDateTime reminderDate;

    /**
     * Session expiry for guest wishlists (auto-cleanup after period)
     */
    @Column(name = "guest_session_expires_at")
    private LocalDateTime guestSessionExpiresAt;

    // Business logic methods

    /**
     * Check if this is a guest wishlist item
     */
    public boolean isGuestItem() {
        return user == null && guestSessionId != null;
    }

    /**
     * Check if this is an authenticated user's item
     */
    public boolean isUserItem() {
        return user != null;
    }

    /**
     * Check if price has dropped since adding
     */
    public boolean isPriceDropped() {
        if (priceWhenAdded == null || product == null) {
            return false;
        }
        BigDecimal currentPrice = product.getEffectivePrice();
        return currentPrice.compareTo(priceWhenAdded) < 0;
    }

    /**
     * Calculate price difference
     */
    public BigDecimal getPriceDifference() {
        if (priceWhenAdded == null || product == null) {
            return BigDecimal.ZERO;
        }
        return priceWhenAdded.subtract(product.getEffectivePrice());
    }

    /**
     * Calculate percentage discount
     */
    public BigDecimal getPriceDropPercentage() {
        if (priceWhenAdded == null || priceWhenAdded.compareTo(BigDecimal.ZERO) == 0 || product == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal difference = getPriceDifference();
        return difference.divide(priceWhenAdded, 4, BigDecimal.ROUND_HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    /**
     * Check if should notify about price drop
     */
    public boolean shouldNotifyPriceDrop() {
        if (!notifyOnPriceDrop || product == null) {
            return false;
        }

        // Notify if target price is met
        if (targetPrice != null) {
            return product.getEffectivePrice().compareTo(targetPrice) <= 0;
        }

        // Or if there's any price drop
        return isPriceDropped();
    }

    /**
     * Check if should notify about stock
     */
    public boolean shouldNotifyStock() {
        return notifyOnStock && product != null && product.isInStock() && !purchased;
    }

    /**
     * Mark item as purchased
     */
    public void markAsPurchased() {
        this.purchased = true;
        this.purchasedAt = LocalDateTime.now();
    }

    /**
     * Check if guest session has expired
     */
    public boolean isGuestSessionExpired() {
        return isGuestItem() && guestSessionExpiresAt != null
                && LocalDateTime.now().isAfter(guestSessionExpiresAt);
    }

    /**
     * Check if reminder is due
     */
    public boolean isReminderDue() {
        return reminderEnabled && reminderDate != null
                && LocalDateTime.now().isAfter(reminderDate);
    }

    @PrePersist
    protected void onCreate() {
        super.onCreate();

        // Set price when added if not set
        if (priceWhenAdded == null && product != null) {
            priceWhenAdded = product.getEffectivePrice();
        }

        // Set guest session expiry (30 days default)
        if (isGuestItem() && guestSessionExpiresAt == null) {
            guestSessionExpiresAt = LocalDateTime.now().plusDays(30);
        }

        // Validate: must have either user or guest session
        if (user == null && guestSessionId == null) {
            throw new IllegalStateException("Wishlist item must have either user or guest session ID");
        }
    }

    @PreUpdate
    protected void onUpdate() {
        super.onUpdate();

        // Update price drop count if price dropped
        if (isPriceDropped() && lastPriceCheck != null) {
            BigDecimal previousPrice = priceWhenAdded;
            BigDecimal currentPrice = product.getEffectivePrice();
            if (currentPrice.compareTo(previousPrice) < 0) {
                priceDropCount++;
            }
        }

        lastPriceCheck = LocalDateTime.now();
    }
}