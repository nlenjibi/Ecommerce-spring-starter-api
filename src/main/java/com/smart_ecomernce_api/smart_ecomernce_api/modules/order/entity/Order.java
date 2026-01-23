package com.smart_ecomernce_api.smart_ecomernce_api.modules.order.entity;

import com.smart_ecomernce_api.smart_ecomernce_api.common.base.BaseEntity;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.cart.entity.Cart;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.user.entity.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Random;
import java.util.Set;

/**
 * Order Entity
 * Represents a customer order created from cart
 */
@Entity
@Table(name = "orders", indexes = {
        @Index(name = "idx_order_number", columnList = "order_number"),
        @Index(name = "idx_order_user", columnList = "user_id"),
        @Index(name = "idx_order_status", columnList = "status"),
        @Index(name = "idx_order_payment_status", columnList = "payment_status"),
        @Index(name = "idx_order_created", columnList = "created_at"),
        @Index(name = "idx_order_user_status", columnList = "user_id, status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order extends BaseEntity {

    @Column(name = "order_number", nullable = false, unique = true, length = 50)
    @NotBlank(message = "Order number is required")
    private String orderNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull(message = "User is required")
    private User user;

    @Column(name = "customer_email", length = 100)
    private String customerEmail;

    @Column(name = "customer_name", length = 200)
    private String customerName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @NotNull(message = "Order status is required")
    @Builder.Default
    private OrderStatus status = OrderStatus.PENDING;

    @CreationTimestamp
    @Column(name = "order_date", nullable = false, updatable = false)
    private LocalDateTime orderDate;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private Set<OrderItem> orderItems = new LinkedHashSet<>();

    @Column(name = "subtotal", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "tax_amount", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(name = "tax_rate", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal taxRate = BigDecimal.ZERO;

    @Column(name = "shipping_cost", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal shippingCost = BigDecimal.ZERO;

    @Column(name = "discount_amount", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "coupon_code", length = 50)
    private String couponCode;

    @Column(name = "coupon_discount", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal couponDiscount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 30)
    @Builder.Default
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", length = 30)
    private PaymentMethod paymentMethod;

    @Column(name = "payment_transaction_id", length = 100)
    private String paymentTransactionId;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "cancellation_reason", columnDefinition = "TEXT")
    private String cancellationReason;

    @Column(name = "refunded_at")
    private LocalDateTime refundedAt;

    @Column(name = "refund_amount", precision = 10, scale = 2)
    private BigDecimal refundAmount;

    @Column(name = "refund_reason", columnDefinition = "TEXT")
    private String refundReason;


    public static Order fromCart(Cart cart, User customer) {
        if (cart == null) {
            throw new IllegalArgumentException("Cart cannot be null");
        }

        if (cart.isEmpty()) {
            throw new IllegalStateException("Cannot create order from empty cart");
        }

        if (customer == null) {
            throw new IllegalArgumentException("Customer cannot be null");
        }

        // Create order
        Order order = Order.builder()
                .user(customer)
                .customerEmail(customer.getEmail())
                .customerName(customer.getFullName())
                .status(OrderStatus.PENDING)
                .paymentStatus(PaymentStatus.PENDING)
                .orderNumber(generateOrderNumber())
                .build();

        // Copy cart items to order items
        cart.getItems().forEach(cartItem -> {
            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .product(cartItem.getProduct())
                    .productName(cartItem.getProduct().getName())
                    .quantity(cartItem.getQuantity())
                    .unitPrice(cartItem.getUnitPrice())
                    .build();

            order.addOrderItem(orderItem);
        });

        // Copy discount/coupon from cart
        if (cart.getCouponCode() != null) {
            order.setCouponCode(cart.getCouponCode());
            order.setCouponDiscount(cart.getDiscountAmount());
        }

        // Calculate totals
        order.calculateTotals();

        return order;
    }

    /**
     * Generate unique order number
     * Format: ORD-YYYYMMDD-XXXXXX (e.g., ORD-20240115-123456)
     */
    private static String generateOrderNumber() {
        String timestamp = java.time.format.DateTimeFormatter
                .ofPattern("yyyyMMdd")
                .format(LocalDateTime.now());

        String randomPart = String.format("%06d", new Random().nextInt(999999));

        return "ORD-" + timestamp + "-" + randomPart;
    }

    // ========================================================================
    // BUSINESS LOGIC METHODS
    // ========================================================================

    /**
     * Add order item
     */
    public void addOrderItem(OrderItem item) {
        if (item == null) {
            throw new IllegalArgumentException("Order item cannot be null");
        }
        orderItems.add(item);
        item.setOrder(this);
    }

    /**
     * Remove order item
     */
    public void removeOrderItem(OrderItem item) {
        orderItems.remove(item);
        item.setOrder(null);
    }

    /**
     * Calculate totals
     */
    public void calculateTotals() {
        // Calculate subtotal from items
        this.subtotal = orderItems.stream()
                .map(OrderItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calculate tax
        if (taxRate != null && taxRate.compareTo(BigDecimal.ZERO) > 0) {
            this.taxAmount = subtotal
                    .multiply(taxRate)
                    .divide(BigDecimal.valueOf(100), 2, BigDecimal.ROUND_HALF_UP);
        }

        // Calculate total
        this.totalAmount = subtotal
                .add(taxAmount != null ? taxAmount : BigDecimal.ZERO)
                .add(shippingCost != null ? shippingCost : BigDecimal.ZERO)
                .subtract(discountAmount != null ? discountAmount : BigDecimal.ZERO)
                .subtract(couponDiscount != null ? couponDiscount : BigDecimal.ZERO);

        // Ensure total is not negative
        if (this.totalAmount.compareTo(BigDecimal.ZERO) < 0) {
            this.totalAmount = BigDecimal.ZERO;
        }
    }

    /**
     * Apply coupon
     */
    public void applyCoupon(String code, BigDecimal discount) {
        this.couponCode = code;
        this.couponDiscount = discount;
        calculateTotals();
    }

    /**
     * Apply tax
     */
    public void applyTax(BigDecimal rate) {
        this.taxRate = rate;
        calculateTotals();
    }

    /**
     * Apply shipping cost
     */
    public void applyShippingCost(BigDecimal cost) {
        this.shippingCost = cost;
        calculateTotals();
    }

    /**
     * Confirm order
     */
    public void confirm() {
        if (this.status != OrderStatus.PENDING) {
            throw new IllegalStateException("Only pending orders can be confirmed");
        }
        this.status = OrderStatus.CONFIRMED;
    }

    /**
     * Mark as processing
     */
    public void process() {
        if (this.status != OrderStatus.CONFIRMED) {
            throw new IllegalStateException("Only confirmed orders can be processed");
        }
        this.status = OrderStatus.PROCESSING;
    }

    /**
     * Mark as shipped
     */
    public void ship() {
        if (this.status != OrderStatus.PROCESSING) {
            throw new IllegalStateException("Only processing orders can be shipped");
        }
        this.status = OrderStatus.SHIPPED;
    }

    /**
     * Mark as out for delivery
     */
    public void outForDelivery() {
        if (this.status != OrderStatus.SHIPPED) {
            throw new IllegalStateException("Only shipped orders can be out for delivery");
        }
        this.status = OrderStatus.OUT_FOR_DELIVERY;
    }

    /**
     * Deliver order
     */
    public void deliver() {
        if (this.status != OrderStatus.OUT_FOR_DELIVERY && this.status != OrderStatus.SHIPPED) {
            throw new IllegalStateException("Cannot deliver order in current status");
        }
        this.status = OrderStatus.DELIVERED;
        this.deliveredAt = LocalDateTime.now();
    }

    /**
     * Cancel order
     */
    public void cancel(String reason) {
        if (!canBeCancelled()) {
            throw new IllegalStateException("Cannot cancel order in current status");
        }
        this.status = OrderStatus.CANCELLED;
        this.cancelledAt = LocalDateTime.now();
        this.cancellationReason = reason;
    }

    /**
     * Refund order
     */
    public void refund(BigDecimal amount, String reason) {
        if (!canBeRefunded()) {
            throw new IllegalStateException("Order cannot be refunded");
        }

        this.status = OrderStatus.REFUNDED;
        this.paymentStatus = amount.compareTo(totalAmount) < 0
                ? PaymentStatus.PARTIALLY_REFUNDED
                : PaymentStatus.REFUNDED;
        this.refundedAt = LocalDateTime.now();
        this.refundAmount = amount;
        this.refundReason = reason;
    }

    /**
     * Mark payment as paid
     */
    public void markAsPaid(String transactionId) {
        this.paymentStatus = PaymentStatus.PAID;
        this.paymentTransactionId = transactionId;
        this.paidAt = LocalDateTime.now();
    }

    /**
     * Mark payment as failed
     */
    public void markPaymentFailed() {
        this.paymentStatus = PaymentStatus.FAILED;
        this.status = OrderStatus.FAILED;
    }

    // ========================================================================
    // VALIDATION METHODS
    // ========================================================================

    /**
     * Check if order can be cancelled
     */
    public boolean canBeCancelled() {
        return status == OrderStatus.PENDING ||
                status == OrderStatus.CONFIRMED ||
                status == OrderStatus.PROCESSING;
    }

    /**
     * Check if order can be refunded
     */
    public boolean canBeRefunded() {
        return (status == OrderStatus.DELIVERED || status == OrderStatus.SHIPPED) &&
                paymentStatus == PaymentStatus.PAID;
    }

    /**
     * Check if order is placed by customer
     */
    public boolean isPlacedBy(User customer) {
        if (customer == null || this.user == null) {
            return false;
        }
        return this.user.getId().equals(customer.getId());
    }

    /**
     * Check if order is completed
     */
    public boolean isCompleted() {
        return status == OrderStatus.DELIVERED;
    }

    /**
     * Check if order is active (not cancelled/failed/refunded)
     */
    public boolean isActive() {
        return status != OrderStatus.CANCELLED &&
                status != OrderStatus.REFUNDED &&
                status != OrderStatus.FAILED;
    }

    /**
     * Check if order is paid
     */
    public boolean isPaid() {
        return paymentStatus == PaymentStatus.PAID;
    }


    /**
     * Get order item count
     */
    public int getItemCount() {
        return orderItems.stream()
                .mapToInt(OrderItem::getQuantity)
                .sum();
    }

    /**
     * Get number of unique product
     */
    public int getUniqueProductCount() {
        return orderItems.size();
    }

    /**
     * Find order item by product ID
     */
    public OrderItem findItemByProductId(Long productId) {
        return orderItems.stream()
                .filter(item -> item.getProduct().getId().equals(productId))
                .findFirst()
                .orElse(null);
    }


    @PrePersist
    protected void onCreate() {
        if (orderNumber == null || orderNumber.isEmpty()) {
            orderNumber = generateOrderNumber();
        }
        calculateTotals();
    }

    @PreUpdate
    protected void onUpdate() {
        calculateTotals();
    }
}