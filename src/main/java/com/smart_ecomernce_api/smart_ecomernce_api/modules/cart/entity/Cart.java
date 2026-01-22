package com.smart_ecomernce_api.smart_ecomernce_api.modules.cart.entity;


import com.smart_ecomernce_api.Smart_ecommerce_api.modules.product.entity.Product;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "carts", indexes = {
        @Index(name = "idx_cart_session", columnList = "session_id"),
        @Index(name = "idx_cart_status", columnList = "status"),
        @Index(name = "idx_cart_created", columnList = "date_created")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cart {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @CreationTimestamp
    @Column(name = "date_created", nullable = false, updatable = false)
    private LocalDateTime dateCreated;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    @Builder.Default
    private CartStatus status = CartStatus.ACTIVE;

    @Column(name = "session_id", length = 100)
    private String sessionId;

    @OneToMany(
            mappedBy = "cart",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    @Builder.Default
    private Set<CartItem> items = new LinkedHashSet<>();  // FIXED: Changed from orderItems to items

    @Column(name = "coupon_code", length = 50)
    private String couponCode;

    @Column(name = "discount_amount", precision = 10, scale = 2)
    private BigDecimal discountAmount;

    // ========================================================================
    // Business Methods
    // ========================================================================

    public BigDecimal getTotalPrice() {
        return items.stream()
                .map(CartItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getFinalPrice() {
        BigDecimal total = getTotalPrice();
        if (discountAmount != null && discountAmount.compareTo(BigDecimal.ZERO) > 0) {
            return total.subtract(discountAmount).max(BigDecimal.ZERO);
        }
        return total;
    }

    public CartItem getItem(Long productId) {
        return items.stream()
                .filter(item -> item.getProduct().getId().equals(productId))
                .findFirst()
                .orElse(null);
    }

    public CartItem addItem(Product product) {
        CartItem cartItem = getItem(product.getId());

        if (cartItem != null) {
            cartItem.setQuantity(cartItem.getQuantity() + 1);
        } else {
            cartItem = CartItem.builder()
                    .product(product)
                    .quantity(1)
                    .unitPrice(product.getPrice())
                    .cart(this)
                    .build();
            items.add(cartItem);
        }

        this.status = CartStatus.ACTIVE;
        return cartItem;
    }

    public CartItem addItem(Product product, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }

        CartItem cartItem = getItem(product.getId());

        if (cartItem != null) {
            cartItem.setQuantity(cartItem.getQuantity() + quantity);
        } else {
            cartItem = CartItem.builder()
                    .product(product)
                    .quantity(quantity)
                    .unitPrice(product.getPrice())
                    .cart(this)
                    .build();
            items.add(cartItem);
        }

        this.status = CartStatus.ACTIVE;
        return cartItem;
    }

    public void updateItemQuantity(Long productId, int quantity) {
        if (quantity <= 0) {
            removeItem(productId);
            return;
        }

        CartItem cartItem = getItem(productId);
        if (cartItem != null) {
            cartItem.setQuantity(quantity);
        }
    }

    public void removeItem(Long productId) {
        CartItem cartItem = getItem(productId);
        if (cartItem != null) {
            items.remove(cartItem);
            cartItem.setCart(null);
        }
    }

    public void clear() {
        items.clear();
        this.couponCode = null;
        this.discountAmount = null;
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    public int getItemCount() {
        return items.stream()
                .mapToInt(CartItem::getQuantity)
                .sum();
    }

    public void markAsAbandoned() {
        this.status = CartStatus.ABANDONED;
    }

    public void markAsConverted() {
        this.status = CartStatus.CONVERTED;
    }

    public boolean isExpired(int days) {
        return dateCreated.plusDays(days).isBefore(LocalDateTime.now());
    }

    public void applyDiscount(String couponCode, BigDecimal discountAmount) {
        this.couponCode = couponCode;
        this.discountAmount = discountAmount;
    }

    public void removeDiscount() {
        this.couponCode = null;
        this.discountAmount = null;
    }
}
