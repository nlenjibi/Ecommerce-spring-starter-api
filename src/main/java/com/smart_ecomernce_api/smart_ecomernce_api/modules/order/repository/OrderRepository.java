package com.smart_ecomernce_api.smart_ecomernce_api.modules.order.repository;

import com.smart_ecomernce_api.smart_ecomernce_api.modules.order.entity.Order;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.order.entity.OrderStats;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.order.entity.OrderStatus;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.order.entity.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderRepository {

    /**
     * Save a new order
     */
    Order save(Order order);

    /**
     * Update an existing order
     */
    Order update(Order order);

    /**
     * Find order by ID
     */
    Optional<Order> findById(Long id);

    /**
     * Find order by order number
     */
    Optional<Order> findByOrderNumber(String orderNumber);

    /**
     * Find all orders with pagination
     */
    List<Order> findAll(int page, int size);

    /**
     * Find orders by user ID with pagination
     */
    List<Order> findByUserId(Long userId, int page, int size);

    /**
     * Find orders by status with pagination
     */
    List<Order> findByStatus(OrderStatus status, int page, int size);

    /**
     * Find orders by user ID and status
     */
    List<Order> findByUserIdAndStatus(Long userId, OrderStatus status, int page, int size);

    /**
     * Find orders by date range
     */
    List<Order> findByDateRange(LocalDateTime startDate, LocalDateTime endDate, int page, int size);

    /**
     * Find recent orders
     */
    List<Order> findRecentOrders(LocalDateTime startDate, int limit);

    /**
     * Find pending payments
     */
    List<Order> findPendingPayments(LocalDateTime cutoffDate);

    /**
     * Count total orders
     */
    long count();

    /**
     * Count orders by status
     */
    long countByStatus(OrderStatus status);

    /**
     * Count orders by payment status
     */
    long countByPaymentStatus(PaymentStatus paymentStatus);

    /**
     * Count user orders
     */
    long countByUserId(Long userId);

    /**
     * Calculate total revenue
     */
    BigDecimal calculateTotalRevenue();

    /**
     * Calculate revenue for a specific period
     */
    BigDecimal calculateRevenue(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Get order statistics
     */
    OrderStats getOrderStatistics();

    /**
     * Check if user has purchased a product
     */
    boolean existsByUserIdAndProductId(Long userId, Long productId);

    /**
     * Delete order by ID
     */
    boolean deleteById(Long id);

    /**
     * Check if order exists
     */
    boolean existsById(Long id);

    /**
     * Check if order number exists
     */
    boolean existsByOrderNumber(String orderNumber);
}