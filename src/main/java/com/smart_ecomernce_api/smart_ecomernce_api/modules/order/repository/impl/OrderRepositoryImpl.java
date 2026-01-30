package com.smart_ecomernce_api.smart_ecomernce_api.modules.order.repository.impl;

import com.smart_ecomernce_api.smart_ecomernce_api.common.utils.JdbcUtils;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.order.entity.*;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.order.repository.OrderRepository;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;

/**
 * JDBC-based Order Repository Implementation
 */
@Repository
public class OrderRepositoryImpl implements OrderRepository {

    private final JdbcUtils jdbcUtils;

    public OrderRepositoryImpl(JdbcUtils jdbcUtils) {
        this.jdbcUtils = jdbcUtils;
    }

    /**
     * RowMapper for Order entity
     */
    private static class OrderRowMapper implements RowMapper<Order> {
        @Override
        public Order mapRow(ResultSet rs, int rowNum) throws SQLException {
            Order order = new Order();
            order.setId(rs.getLong("id"));
            order.setOrderNumber(rs.getString("order_number"));
            order.setCustomerEmail(rs.getString("customer_email"));
            order.setCustomerName(rs.getString("customer_name"));
            order.setStatus(OrderStatus.valueOf(rs.getString("status")));
            order.setOrderDate(rs.getTimestamp("order_date").toLocalDateTime());
            order.setSubtotal(rs.getBigDecimal("subtotal"));
            order.setTaxAmount(rs.getBigDecimal("tax_amount"));
            order.setTaxRate(rs.getBigDecimal("tax_rate"));
            order.setShippingCost(rs.getBigDecimal("shipping_cost"));
            order.setDiscountAmount(rs.getBigDecimal("discount_amount"));
            order.setTotalAmount(rs.getBigDecimal("total_amount"));
            order.setCouponCode(rs.getString("coupon_code"));
            order.setCouponDiscount(rs.getBigDecimal("coupon_discount"));
            order.setPaymentStatus(PaymentStatus.valueOf(rs.getString("payment_status")));

            String paymentMethodStr = rs.getString("payment_method");
            if (paymentMethodStr != null) {
                order.setPaymentMethod(PaymentMethod.valueOf(paymentMethodStr));
            }

            order.setPaymentTransactionId(rs.getString("payment_transaction_id"));

            if (rs.getTimestamp("paid_at") != null) {
                order.setPaidAt(rs.getTimestamp("paid_at").toLocalDateTime());
            }
            if (rs.getTimestamp("delivered_at") != null) {
                order.setDeliveredAt(rs.getTimestamp("delivered_at").toLocalDateTime());
            }
            if (rs.getTimestamp("cancelled_at") != null) {
                order.setCancelledAt(rs.getTimestamp("cancelled_at").toLocalDateTime());
            }

            order.setCancellationReason(rs.getString("cancellation_reason"));

            if (rs.getTimestamp("refunded_at") != null) {
                order.setRefundedAt(rs.getTimestamp("refunded_at").toLocalDateTime());
            }

            order.setRefundAmount(rs.getBigDecimal("refund_amount"));
            order.setRefundReason(rs.getString("refund_reason"));

            if (rs.getTimestamp("created_at") != null) {
                order.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
            }
            if (rs.getTimestamp("updated_at") != null) {
                order.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
            }

            return order;
        }
    }

    @Override
    public Order save(Order order) {
        String sql = """
            INSERT INTO orders (
                order_number, user_id, customer_email, customer_name, status, order_date,
                subtotal, tax_amount, tax_rate, shipping_cost, discount_amount, total_amount,
                coupon_code, coupon_discount, payment_status, payment_method, payment_transaction_id,
                paid_at, delivered_at, cancelled_at, cancellation_reason, refunded_at, refund_amount,
                refund_reason, created_at, updated_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        JdbcUtils.QueryResult result = jdbcUtils.executePreparedQuery(sql,
                order.getOrderNumber(),
                order.getUser() != null ? order.getUser().getId() : null,
                order.getCustomerEmail(),
                order.getCustomerName(),
                order.getStatus().name(),
                order.getOrderDate(),
                order.getSubtotal(),
                order.getTaxAmount(),
                order.getTaxRate(),
                order.getShippingCost(),
                order.getDiscountAmount(),
                order.getTotalAmount(),
                order.getCouponCode(),
                order.getCouponDiscount(),
                order.getPaymentStatus().name(),
                order.getPaymentMethod() != null ? order.getPaymentMethod().name() : null,
                order.getPaymentTransactionId(),
                order.getPaidAt(),
                order.getDeliveredAt(),
                order.getCancelledAt(),
                order.getCancellationReason(),
                order.getRefundedAt(),
                order.getRefundAmount(),
                order.getRefundReason(),
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        if (result.getGeneratedKey() != null) {
            order.setId(result.getGeneratedKey());
        }

        return order;
    }

    @Override
    public Order update(Order order) {
        String sql = """
            UPDATE orders SET
                order_number = ?, customer_email = ?, customer_name = ?, status = ?,
                subtotal = ?, tax_amount = ?, tax_rate = ?, shipping_cost = ?, discount_amount = ?,
                total_amount = ?, coupon_code = ?, coupon_discount = ?, payment_status = ?,
                payment_method = ?, payment_transaction_id = ?, paid_at = ?, delivered_at = ?,
                cancelled_at = ?, cancellation_reason = ?, refunded_at = ?, refund_amount = ?,
                refund_reason = ?, updated_at = ?
            WHERE id = ?
        """;

        jdbcUtils.executePreparedQuery(sql,
                order.getOrderNumber(),
                order.getCustomerEmail(),
                order.getCustomerName(),
                order.getStatus().name(),
                order.getSubtotal(),
                order.getTaxAmount(),
                order.getTaxRate(),
                order.getShippingCost(),
                order.getDiscountAmount(),
                order.getTotalAmount(),
                order.getCouponCode(),
                order.getCouponDiscount(),
                order.getPaymentStatus().name(),
                order.getPaymentMethod() != null ? order.getPaymentMethod().name() : null,
                order.getPaymentTransactionId(),
                order.getPaidAt(),
                order.getDeliveredAt(),
                order.getCancelledAt(),
                order.getCancellationReason(),
                order.getRefundedAt(),
                order.getRefundAmount(),
                order.getRefundReason(),
                LocalDateTime.now(),
                order.getId()
        );

        return order;
    }

    @Override
    public Optional<Order> findById(Long id) {
        String sql = "SELECT * FROM orders WHERE id = ?";
        List<Order> orders = jdbcUtils.query(sql, new OrderRowMapper(), id);
        return orders.isEmpty() ? Optional.empty() : Optional.of(orders.get(0));
    }

    @Override
    public Optional<Order> findByOrderNumber(String orderNumber) {
        String sql = "SELECT * FROM orders WHERE order_number = ?";
        List<Order> orders = jdbcUtils.query(sql, new OrderRowMapper(), orderNumber);
        return orders.isEmpty() ? Optional.empty() : Optional.of(orders.get(0));
    }

    @Override
    public List<Order> findAll(int page, int size) {
        String sql = "SELECT * FROM orders ORDER BY order_date DESC LIMIT ? OFFSET ?";
        return jdbcUtils.query(sql, new OrderRowMapper(), size, page * size);
    }

    @Override
    public List<Order> findByUserId(Long userId, int page, int size) {
        String sql = "SELECT * FROM orders WHERE user_id = ? ORDER BY order_date DESC LIMIT ? OFFSET ?";
        return jdbcUtils.query(sql, new OrderRowMapper(), userId, size, page * size);
    }

    @Override
    public List<Order> findByStatus(OrderStatus status, int page, int size) {
        String sql = "SELECT * FROM orders WHERE status = ? ORDER BY order_date DESC LIMIT ? OFFSET ?";
        return jdbcUtils.query(sql, new OrderRowMapper(), status.name(), size, page * size);
    }

    @Override
    public List<Order> findByUserIdAndStatus(Long userId, OrderStatus status, int page, int size) {
        String sql = "SELECT * FROM orders WHERE user_id = ? AND status = ? ORDER BY order_date DESC LIMIT ? OFFSET ?";
        return jdbcUtils.query(sql, new OrderRowMapper(), userId, status.name(), size, page * size);
    }

    @Override
    public List<Order> findByDateRange(LocalDateTime startDate, LocalDateTime endDate, int page, int size) {
        String sql = """
            SELECT * FROM orders 
            WHERE order_date BETWEEN ? AND ? 
            ORDER BY order_date DESC 
            LIMIT ? OFFSET ?
        """;
        return jdbcUtils.query(sql, new OrderRowMapper(), startDate, endDate, size, page * size);
    }

    @Override
    public List<Order> findRecentOrders(LocalDateTime startDate, int limit) {
        String sql = "SELECT * FROM orders WHERE order_date >= ? ORDER BY order_date DESC LIMIT ?";
        return jdbcUtils.query(sql, new OrderRowMapper(), startDate, limit);
    }

    @Override
    public List<Order> findPendingPayments(LocalDateTime cutoffDate) {
        String sql = """
            SELECT * FROM orders 
            WHERE payment_status = 'PENDING' AND order_date < ?
            ORDER BY order_date DESC
        """;
        return jdbcUtils.query(sql, new OrderRowMapper(), cutoffDate);
    }

    @Override
    public long count() {
        String sql = "SELECT COUNT(*) FROM orders";
        Long count = jdbcUtils.queryForObject(sql, Long.class);
        return count != null ? count : 0L;
    }

    @Override
    public long countByStatus(OrderStatus status) {
        String sql = "SELECT COUNT(*) FROM orders WHERE status = ?";
        Long count = jdbcUtils.queryForObject(sql, Long.class, status.name());
        return count != null ? count : 0L;
    }

    @Override
    public long countByPaymentStatus(PaymentStatus paymentStatus) {
        String sql = "SELECT COUNT(*) FROM orders WHERE payment_status = ?";
        Long count = jdbcUtils.queryForObject(sql, Long.class, paymentStatus.name());
        return count != null ? count : 0L;
    }

    @Override
    public long countByUserId(Long userId) {
        String sql = "SELECT COUNT(*) FROM orders WHERE user_id = ?";
        Long count = jdbcUtils.queryForObject(sql, Long.class, userId);
        return count != null ? count : 0L;
    }

    @Override
    public BigDecimal calculateTotalRevenue() {
        String sql = "SELECT COALESCE(SUM(total_amount), 0) FROM orders WHERE payment_status = 'PAID'";
        BigDecimal revenue = jdbcUtils.queryForObject(sql, BigDecimal.class);
        return revenue != null ? revenue : BigDecimal.ZERO;
    }

    @Override
    public BigDecimal calculateRevenue(LocalDateTime startDate, LocalDateTime endDate) {
        String sql = """
            SELECT COALESCE(SUM(total_amount), 0) 
            FROM orders 
            WHERE payment_status = 'PAID' AND order_date BETWEEN ? AND ?
        """;
        BigDecimal revenue = jdbcUtils.queryForObject(sql, BigDecimal.class, startDate, endDate);
        return revenue != null ? revenue : BigDecimal.ZERO;
    }

    @Override
    public OrderStats getOrderStatistics() {
        String countSql = """
            SELECT 
                COUNT(*) as total_orders,
                SUM(CASE WHEN status = 'PENDING' THEN 1 ELSE 0 END) as pending_orders,
                SUM(CASE WHEN status = 'PROCESSING' THEN 1 ELSE 0 END) as processing_orders,
                SUM(CASE WHEN status = 'SHIPPED' THEN 1 ELSE 0 END) as shipped_orders,
                SUM(CASE WHEN status = 'DELIVERED' THEN 1 ELSE 0 END) as delivered_orders,
                SUM(CASE WHEN status = 'CANCELLED' THEN 1 ELSE 0 END) as cancelled_orders,
                COALESCE(SUM(CASE WHEN payment_status = 'PAID' THEN total_amount ELSE 0 END), 0) as total_revenue
            FROM orders
        """;

        Map<String, Object> statsMap = jdbcUtils.queryForMap(countSql);

        // Calculate monthly revenue (current month)
        LocalDateTime startOfMonth = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        BigDecimal monthlyRevenue = calculateRevenue(startOfMonth, LocalDateTime.now());

        return OrderStats.builder()
                .totalOrders(getLongValue(statsMap, "total_orders"))
                .pendingOrders(getLongValue(statsMap, "pending_orders"))
                .processingOrders(getLongValue(statsMap, "processing_orders"))
                .shippedOrders(getLongValue(statsMap, "shipped_orders"))
                .deliveredOrders(getLongValue(statsMap, "delivered_orders"))
                .cancelledOrders(getLongValue(statsMap, "cancelled_orders"))
                .totalRevenue((BigDecimal) statsMap.getOrDefault("total_revenue", BigDecimal.ZERO))
                .monthlyRevenue(monthlyRevenue)
                .build();
    }

    @Override
    public boolean existsByUserIdAndProductId(Long userId, Long productId) {
        String sql = """
            SELECT COUNT(*) FROM order_items oi
            JOIN orders o ON oi.order_id = o.id
            WHERE o.user_id = ? AND oi.product_id = ? AND o.status = 'DELIVERED'
        """;
        Long count = jdbcUtils.queryForObject(sql, Long.class, userId, productId);
        return count != null && count > 0;
    }

    @Override
    public boolean deleteById(Long id) {
        String sql = "DELETE FROM orders WHERE id = ?";
        JdbcUtils.QueryResult result = jdbcUtils.executePreparedQuery(sql, id);
        return result.getAffectedRows() > 0;
    }

    @Override
    public boolean existsById(Long id) {
        String sql = "SELECT COUNT(*) FROM orders WHERE id = ?";
        Long count = jdbcUtils.queryForObject(sql, Long.class, id);
        return count != null && count > 0;
    }

    @Override
    public boolean existsByOrderNumber(String orderNumber) {
        String sql = "SELECT COUNT(*) FROM orders WHERE order_number = ?";
        Long count = jdbcUtils.queryForObject(sql, Long.class, orderNumber);
        return count != null && count > 0;
    }

    /**
     * Helper method to safely extract Long values from Map
     */
    private Long getLongValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            return 0L;
        }
        if (value instanceof Long) {
            return (Long) value;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return 0L;
    }
}