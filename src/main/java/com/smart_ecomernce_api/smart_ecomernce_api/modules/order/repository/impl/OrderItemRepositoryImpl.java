package com.smart_ecomernce_api.smart_ecomernce_api.modules.order.repository.impl;

import com.smart_ecomernce_api.smart_ecomernce_api.common.utils.JdbcUtils;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.order.entity.OrderItem;

import com.smart_ecomernce_api.smart_ecomernce_api.modules.order.repository.OrderItemRepository;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;

/**
 * JDBC-based OrderItem Repository Implementation
 */
@Repository
public class OrderItemRepositoryImpl implements OrderItemRepository {

    private final JdbcUtils jdbcUtils;

    public OrderItemRepositoryImpl(JdbcUtils jdbcUtils) {
        this.jdbcUtils = jdbcUtils;
    }

    /**
     * RowMapper for OrderItem entity
     */
    private static class OrderItemRowMapper implements RowMapper<OrderItem> {
        @Override
        public OrderItem mapRow(ResultSet rs, int rowNum) throws SQLException {
            OrderItem orderItem = new OrderItem();
            orderItem.setId(rs.getLong("id"));
            orderItem.setProductName(rs.getString("product_name"));
            orderItem.setQuantity(rs.getInt("quantity"));
            orderItem.setUnitPrice(rs.getBigDecimal("unit_price"));
            orderItem.setDiscount(rs.getBigDecimal("discount"));
            orderItem.setProductImageUrl(rs.getString("product_image_url"));
            orderItem.setTotalPrice(rs.getBigDecimal("total_price"));

            if (rs.getTimestamp("created_at") != null) {
                orderItem.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
            }
            if (rs.getTimestamp("updated_at") != null) {
                orderItem.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
            }

            return orderItem;
        }
    }

    @Override
    public OrderItem save(OrderItem orderItem) {
        String sql = """
            INSERT INTO order_items (
                order_id, product_id, product_name, quantity, unit_price, 
                discount, product_image_url, total_price, created_at, updated_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        BigDecimal totalPrice = calculateTotalPrice(orderItem);

        JdbcUtils.QueryResult result = jdbcUtils.executePreparedQuery(sql,
                orderItem.getOrder() != null ? orderItem.getOrder().getId() : null,
                orderItem.getProduct() != null ? orderItem.getProduct().getId() : null,
                orderItem.getProductName(),
                orderItem.getQuantity(),
                orderItem.getUnitPrice(),
                orderItem.getDiscount(),
                orderItem.getProductImageUrl(),
                totalPrice,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        if (result.getGeneratedKey() != null) {
            orderItem.setId(result.getGeneratedKey());
        }
        orderItem.setTotalPrice(totalPrice);

        return orderItem;
    }

    @Override
    public OrderItem update(OrderItem orderItem) {
        String sql = """
            UPDATE order_items SET
                product_name = ?, quantity = ?, unit_price = ?, 
                discount = ?, product_image_url = ?, total_price = ?, updated_at = ?
            WHERE id = ?
        """;

        BigDecimal totalPrice = calculateTotalPrice(orderItem);

        jdbcUtils.executePreparedQuery(sql,
                orderItem.getProductName(),
                orderItem.getQuantity(),
                orderItem.getUnitPrice(),
                orderItem.getDiscount(),
                orderItem.getProductImageUrl(),
                totalPrice,
                LocalDateTime.now(),
                orderItem.getId()
        );

        orderItem.setTotalPrice(totalPrice);
        return orderItem;
    }

    @Override
    public Optional<OrderItem> findById(Long id) {
        String sql = "SELECT * FROM order_items WHERE id = ?";
        List<OrderItem> items = jdbcUtils.query(sql, new OrderItemRowMapper(), id);
        return items.isEmpty() ? Optional.empty() : Optional.of(items.get(0));
    }

    @Override
    public List<OrderItem> findAll() {
        String sql = "SELECT * FROM order_items ORDER BY created_at DESC";
        return jdbcUtils.query(sql, new OrderItemRowMapper());
    }

    @Override
    public List<OrderItem> findByOrderId(Long orderId) {
        String sql = "SELECT * FROM order_items WHERE order_id = ? ORDER BY created_at";
        return jdbcUtils.query(sql, new OrderItemRowMapper(), orderId);
    }

    @Override
    public List<OrderItem> findByProductId(Long productId) {
        String sql = "SELECT * FROM order_items WHERE product_id = ? ORDER BY created_at DESC";
        return jdbcUtils.query(sql, new OrderItemRowMapper(), productId);
    }

    @Override
    public List<Map<String, Object>> findBestSellingProducts(int limit) {
        String sql = """
            SELECT 
                oi.product_id,
                oi.product_name,
                SUM(oi.quantity) as total_quantity_sold,
                COUNT(DISTINCT oi.order_id) as total_orders,
                SUM(oi.total_price) as total_revenue
            FROM order_items oi
            JOIN orders o ON oi.order_id = o.id
            WHERE o.status = 'DELIVERED'
            GROUP BY oi.product_id, oi.product_name
            ORDER BY total_quantity_sold DESC
            LIMIT ?
        """;

        JdbcUtils.QueryResult result = jdbcUtils.executePreparedQuery(sql, limit);
        return result.getResultSet();
    }

    @Override
    public Long getTotalQuantitySoldByProductId(Long productId) {
        String sql = """
            SELECT COALESCE(SUM(oi.quantity), 0) as total_quantity
            FROM order_items oi
            JOIN orders o ON oi.order_id = o.id
            WHERE oi.product_id = ? AND o.status = 'DELIVERED'
        """;

        Long quantity = jdbcUtils.queryForObject(sql, Long.class, productId);
        return quantity != null ? quantity : 0L;
    }

    @Override
    public BigDecimal getTotalRevenueByProductId(Long productId) {
        String sql = """
            SELECT COALESCE(SUM(oi.total_price), 0) as total_revenue
            FROM order_items oi
            JOIN orders o ON oi.order_id = o.id
            WHERE oi.product_id = ? AND o.payment_status = 'PAID'
        """;

        BigDecimal revenue = jdbcUtils.queryForObject(sql, BigDecimal.class, productId);
        return revenue != null ? revenue : BigDecimal.ZERO;
    }

    @Override
    public int countByOrderId(Long orderId) {
        String sql = "SELECT COUNT(*) FROM order_items WHERE order_id = ?";
        Long count = jdbcUtils.queryForObject(sql, Long.class, orderId);
        return count != null ? count.intValue() : 0;
    }

    @Override
    public boolean deleteById(Long id) {
        String sql = "DELETE FROM order_items WHERE id = ?";
        JdbcUtils.QueryResult result = jdbcUtils.executePreparedQuery(sql, id);
        return result.getAffectedRows() > 0;
    }

    @Override
    public int deleteByOrderId(Long orderId) {
        String sql = "DELETE FROM order_items WHERE order_id = ?";
        JdbcUtils.QueryResult result = jdbcUtils.executePreparedQuery(sql, orderId);
        return result.getAffectedRows();
    }

    @Override
    public boolean existsById(Long id) {
        String sql = "SELECT COUNT(*) FROM order_items WHERE id = ?";
        Long count = jdbcUtils.queryForObject(sql, Long.class, id);
        return count != null && count > 0;
    }

    @Override
    public List<OrderItem> saveAll(List<OrderItem> orderItems) {
        List<OrderItem> savedItems = new ArrayList<>();

        for (OrderItem item : orderItems) {
            OrderItem savedItem = save(item);
            savedItems.add(savedItem);
        }

        return savedItems;
    }

    /**
     * Helper method to calculate total price for an order item
     */
    private BigDecimal calculateTotalPrice(OrderItem orderItem) {
        BigDecimal total = orderItem.getUnitPrice()
                .multiply(BigDecimal.valueOf(orderItem.getQuantity()));

        if (orderItem.getDiscount() != null && orderItem.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
            total = total.subtract(orderItem.getDiscount());
        }

        return total.max(BigDecimal.ZERO);
    }
}