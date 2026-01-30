package com.smart_ecomernce_api.smart_ecomernce_api.modules.order.repository;

import com.smart_ecomernce_api.smart_ecomernce_api.modules.order.entity.OrderItem;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * JDBC-based OrderItem Repository Interface
 */
public interface OrderItemRepository {

    /**
     * Save a new order item
     */
    OrderItem save(OrderItem orderItem);

    /**
     * Update an existing order item
     */
    OrderItem update(OrderItem orderItem);

    /**
     * Find order item by ID
     */
    Optional<OrderItem> findById(Long id);

    /**
     * Find all order items
     */
    List<OrderItem> findAll();

    /**
     * Find order items by order ID
     */
    List<OrderItem> findByOrderId(Long orderId);

    /**
     * Find order items by product ID
     */
    List<OrderItem> findByProductId(Long productId);

    /**
     * Find best selling products
     */
    List<Map<String, Object>> findBestSellingProducts(int limit);

    /**
     * Get total quantity sold for a product
     */
    Long getTotalQuantitySoldByProductId(Long productId);

    /**
     * Get total revenue for a product
     */
    java.math.BigDecimal getTotalRevenueByProductId(Long productId);

    /**
     * Count items in an order
     */
    int countByOrderId(Long orderId);

    /**
     * Delete order item by ID
     */
    boolean deleteById(Long id);

    /**
     * Delete all items for an order
     */
    int deleteByOrderId(Long orderId);

    /**
     * Check if order item exists
     */
    boolean existsById(Long id);

    /**
     * Batch save order items
     */
    List<OrderItem> saveAll(List<OrderItem> orderItems);
}