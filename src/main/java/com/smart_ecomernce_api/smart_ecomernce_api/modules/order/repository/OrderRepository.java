package com.smart_ecomernce_api.smart_ecomernce_api.modules.order.repository;

import com.smart_ecomernce_api.Smart_ecommerce_api.modules.order.entity.Order;
import com.smart_ecomernce_api.Smart_ecommerce_api.modules.order.entity.OrderStatus;
import com.smart_ecomernce_api.Smart_ecommerce_api.modules.order.entity.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    // Find by order number
    Optional<Order> findByOrderNumber(String orderNumber);

    // Find user orders
    Page<Order> findByUserIdOrderByOrderDateDesc(Long userId, Pageable pageable);

    // Find by status
    Page<Order> findByStatusOrderByOrderDateDesc(OrderStatus status, Pageable pageable);

    // Find user orders by status
    Page<Order> findByUserIdAndStatusOrderByOrderDateDesc(
            Long userId, OrderStatus status, Pageable pageable);

    // Count user orders
    long countByUserId(Long userId);

    // Count orders by status
    long countByStatus(OrderStatus status);

    // Find recent orders
    @Query("SELECT o FROM Order o WHERE o.orderDate >= :startDate ORDER BY o.orderDate DESC")
    List<Order> findRecentOrders(@Param("startDate") LocalDateTime startDate);

    // Find orders by payment status
    Page<Order> findByPaymentStatusOrderByOrderDateDesc(
            PaymentStatus paymentStatus, Pageable pageable);

    // Find pending payments
    @Query("SELECT o FROM Order o WHERE o.paymentStatus = 'PENDING' " +
            "AND o.orderDate < :cutoffDate")
    List<Order> findPendingPayments(@Param("cutoffDate") LocalDateTime cutoffDate);

    // Calculate total revenue
    @Query("SELECT SUM(o.totalAmount) FROM Order o WHERE o.paymentStatus = 'PAID'")
    BigDecimal calculateTotalRevenue();

    // Calculate revenue for period
    @Query("SELECT SUM(o.totalAmount) FROM Order o WHERE o.paymentStatus = 'PAID' " +
            "AND o.orderDate BETWEEN :startDate AND :endDate")
    BigDecimal calculateRevenue(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    // Find orders by date range
    @Query("SELECT o FROM Order o WHERE o.orderDate BETWEEN :startDate AND :endDate " +
            "ORDER BY o.orderDate DESC")
    Page<Order> findOrdersByDateRange(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );

    // Check if user has purchased product
    @Query("SELECT CASE WHEN COUNT(oi) > 0 THEN true ELSE false END " +
            "FROM OrderItem oi WHERE oi.order.user.id = :userId " +
            "AND oi.product.id = :productId " +
            "AND oi.order.status = 'DELIVERED'")
    boolean existsByUserIdAndProductId(
            @Param("userId") Long userId,
            @Param("productId") Long productId
    );

    // Get order statistics
    @Query("SELECT o.status, COUNT(o) FROM Order o GROUP BY o.status")
    List<Object[]> getOrderStatistics();


    @Query("SELECT o FROM Order o WHERE o.user.id = :userId ORDER BY o.createdAt DESC")
    Page<Order> findByUserId(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT o FROM Order o WHERE o.status = :status ORDER BY o.createdAt DESC")
    Page<Order> findByStatus(@Param("status") OrderStatus status, Pageable pageable);
    long countByPaymentStatus(PaymentStatus paymentStatus);
}