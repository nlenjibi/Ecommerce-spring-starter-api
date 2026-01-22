package com.smart_ecomernce_api.smart_ecomernce_api.modules.product.entity;


import com.smart_ecomernce_api.Smart_ecommerce_api.common.base.BaseEntity;
import com.smart_ecomernce_api.Smart_ecommerce_api.modules.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Wishlist Item Entity
 * Represents a single product in a user's wishlist
 */
@Entity
@Table(
        name = "wishlist_items",
        indexes = {
                @Index(name = "idx_wishlist_user", columnList = "user_id"),
                @Index(name = "idx_wishlist_product", columnList = "product_id"),
                @Index(name = "idx_wishlist_created", columnList = "created_at")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_product", columnNames = {"user_id", "product_id"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WishlistItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    /**
     * When the product was added to wishlist
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

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

    // Business logic methods

    public boolean isPriceDropped() {
        if (priceWhenAdded == null) {
            return false;
        }
        BigDecimal currentPrice = product.getEffectivePrice();
        return currentPrice.compareTo(priceWhenAdded) < 0;
    }

    public BigDecimal getPriceDifference() {
        if (priceWhenAdded == null) {
            return BigDecimal.ZERO;
        }
        return priceWhenAdded.subtract(product.getEffectivePrice());
    }

    public boolean shouldNotifyPriceDrop() {
        if (!notifyOnPriceDrop || targetPrice == null) {
            return false;
        }
        return product.getEffectivePrice().compareTo(targetPrice) <= 0;
    }

    public boolean shouldNotifyStock() {
        return notifyOnStock && product.isInStock() && !purchased;
    }

    public void markAsPurchased() {
        this.purchased = true;
        this.purchasedAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        if (priceWhenAdded == null && product != null) {
            priceWhenAdded = product.getEffectivePrice();
        }
    }
}