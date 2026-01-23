package com.smart_ecomernce_api.smart_ecomernce_api.modules.order.entity;


import com.smart_ecomernce_api.smart_ecomernce_api.common.base.BaseEntity;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.product.entity.Product;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.math.BigDecimal;

/**
 * Order Item Entity
 */
@Entity
@Table(name = "order_items", indexes = {
        @Index(name = "idx_order_item_order", columnList = "order_id"),
        @Index(name = "idx_order_item_product", columnList = "product_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    @NotNull(message = "Order is required")
    private  Order order;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    @NotNull(message = "Product is required")
    private Product product;

    /**
     * Store product name at time of order (in case product is deleted/renamed)
     */
    @Column(name = "product_name", nullable = false, length = 200)
    private String productName;

    /**
     * Quantity ordered
     */
    @Column(name = "quantity", nullable = false)
    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be positive")
    private Integer quantity;

    /**
     * Unit price at time of order (in case price changes)
     */
    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    @NotNull(message = "Unit price is required")
    @Positive(message = "Unit price must be positive")
    private BigDecimal unitPrice;

    /**
     * Discount applied to this item
     */
    @Column(name = "discount", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal discount = BigDecimal.ZERO;

    @Column(name = "product_image_url")
    private String productImageUrl;

    @Column(name = "total_price", nullable = false, precision = 10, scale = 2)
    @NotNull(message = "Total price is required")
    @Positive(message = "Total price must be positive")
    private BigDecimal totalPrice;


    /**
     * Calculate total price for this item
     */
    public BigDecimal getTotalPrice() {
        BigDecimal total = unitPrice.multiply(BigDecimal.valueOf(quantity));
        if (discount != null && discount.compareTo(BigDecimal.ZERO) > 0) {
            total = total.subtract(discount);
        }
        return total.max(BigDecimal.ZERO);
    }

    @PrePersist
    protected void onCreate() {
        if (productName == null && product != null) {
            productName = product.getName();
        }
    }


    /**
     * Get effective unit price (with discount)
     */
    public BigDecimal getEffectiveUnitPrice() {
        if (discount != null && discount.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal totalDiscount = discount.divide(
                    BigDecimal.valueOf(quantity), 2, BigDecimal.ROUND_HALF_UP
            );
            return unitPrice.subtract(totalDiscount);
        }
        return unitPrice;
    }


}
