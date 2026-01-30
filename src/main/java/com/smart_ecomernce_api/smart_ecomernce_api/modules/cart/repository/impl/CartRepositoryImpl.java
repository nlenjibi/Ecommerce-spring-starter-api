package com.smart_ecomernce_api.smart_ecomernce_api.modules.cart.repository.impl;

import com.smart_ecomernce_api.smart_ecomernce_api.common.utils.JdbcUtils;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.cart.entity.Cart;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.cart.entity.CartItem;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.cart.entity.CartStatus;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.cart.repository.CartRepository;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.product.entity.Product;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;

/**
 * JDBC Implementation of CartJdbcRepository
 * Uses JdbcUtils for all database operations
 */
@Repository
public class CartRepositoryImpl implements CartRepository {

    private static final Logger logger = LoggerFactory.getLogger(CartRepositoryImpl.class);
    private final JdbcUtils jdbcUtils;

    public CartRepositoryImpl(JdbcUtils jdbcUtils) {
        this.jdbcUtils = jdbcUtils;
    }

    // ========================================================================
    // Cart CRUD Operations
    // ========================================================================

    @Override
    @Transactional
    public Cart save(Cart cart) {
        String sql = """
            INSERT INTO carts (id, date_created, updated_at, status, session_id, coupon_code, discount_amount)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;

        UUID id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        JdbcUtils.QueryResult result = jdbcUtils.executePreparedQuery(
                sql,
                id,
                Timestamp.valueOf(now),
                Timestamp.valueOf(now),
                cart.getStatus() != null ? cart.getStatus().name() : CartStatus.ACTIVE.name(),
                cart.getSessionId(),
                cart.getCouponCode(),
                cart.getDiscountAmount()
        );

        if (result.hasError()) {
            logger.error("Error saving cart: {}", result.getError());
            throw new RuntimeException("Failed to save cart: " + result.getError());
        }

        cart.setId(id);
        cart.setDateCreated(now);
        cart.setUpdatedAt(now);

        // Save cart items if any
        if (cart.getItems() != null && !cart.getItems().isEmpty()) {
            for (CartItem item : cart.getItems()) {
                item.setCart(cart);
                saveCartItem(item);
            }
        }

        return cart;
    }

    @Override
    @Transactional
    public Cart update(Cart cart) {
        String sql = """
            UPDATE carts 
            SET updated_at = ?, status = ?, session_id = ?, coupon_code = ?, discount_amount = ?
            WHERE id = ?
            """;

        LocalDateTime now = LocalDateTime.now();

        JdbcUtils.QueryResult result = jdbcUtils.executePreparedQuery(
                sql,
                Timestamp.valueOf(now),
                cart.getStatus().name(),
                cart.getSessionId(),
                cart.getCouponCode(),
                cart.getDiscountAmount(),
                cart.getId()
        );

        if (result.hasError()) {
            logger.error("Error updating cart: {}", result.getError());
            throw new RuntimeException("Failed to update cart: " + result.getError());
        }

        cart.setUpdatedAt(now);
        return cart;
    }

    @Override
    public Optional<Cart> findById(UUID id) {
        String sql = """
            SELECT id, date_created, updated_at, status, session_id, coupon_code, discount_amount
            FROM carts
            WHERE id = ?
            """;

        List<Cart> carts = jdbcUtils.query(sql, new CartRowMapper(), id);
        return carts.isEmpty() ? Optional.empty() : Optional.of(carts.get(0));
    }

    @Override
    public Optional<Cart> findByIdWithItems(UUID id) {
        Optional<Cart> cartOpt = findById(id);
        if (cartOpt.isEmpty()) {
            return Optional.empty();
        }

        Cart cart = cartOpt.get();
        List<CartItem> items = findCartItemsByCartId(id);
        cart.getItems().clear();
        cart.getItems().addAll(items);

        return Optional.of(cart);
    }

    @Override
    public Optional<Cart> findBySessionId(String sessionId) {
        String sql = """
            SELECT id, date_created, updated_at, status, session_id, coupon_code, discount_amount
            FROM carts
            WHERE session_id = ?
            """;

        List<Cart> carts = jdbcUtils.query(sql, new CartRowMapper(), sessionId);
        return carts.isEmpty() ? Optional.empty() : Optional.of(carts.get(0));
    }



    @Override
    public List<Cart> findAbandonedCartsBefore(LocalDateTime cutoffDate) {
        String sql = """
            SELECT id, date_created, updated_at, status, session_id, coupon_code, discount_amount
            FROM carts
            WHERE status = ? AND date_created < ?
            ORDER BY date_created DESC
            """;

        return jdbcUtils.query(sql, new CartRowMapper(),
                CartStatus.ABANDONED.name(),
                Timestamp.valueOf(cutoffDate));
    }



    @Override
    public List<Cart> findEmptyCartsBefore(LocalDateTime cutoffDate) {
        String sql = """
            SELECT c.id, c.date_created, c.updated_at, c.status, c.session_id, c.coupon_code, c.discount_amount
            FROM carts c
            LEFT JOIN cart_items ci ON c.id = ci.cart_id
            WHERE c.status = ? AND c.date_created < ?
            GROUP BY c.id, c.date_created, c.updated_at, c.status, c.session_id, c.coupon_code, c.discount_amount
            HAVING COUNT(ci.id) = 0
            ORDER BY c.date_created DESC
            """;

        return jdbcUtils.query(sql, new CartRowMapper(),
                CartStatus.ACTIVE.name(),
                Timestamp.valueOf(cutoffDate));
    }






    @Transactional
    public int deleteByStatusAndCreatedBefore(CartStatus status, LocalDateTime cutoffDate) {
        // First delete cart items for these carts
        String deleteItemsSql = """
            DELETE FROM cart_items
            WHERE cart_id IN (
                SELECT id FROM carts 
                WHERE status = ? AND date_created < ?
            )
            """;

        jdbcUtils.executePreparedQuery(deleteItemsSql, status.name(), Timestamp.valueOf(cutoffDate));

        // Then delete the carts
        String deleteCartsSql = """
            DELETE FROM carts
            WHERE status = ? AND date_created < ?
            """;

        JdbcUtils.QueryResult result = jdbcUtils.executePreparedQuery(
                deleteCartsSql,
                status.name(),
                Timestamp.valueOf(cutoffDate)
        );

        return result.getAffectedRows();
    }

    @Transactional
    public int updateStatusForCartsUpdatedBefore(CartStatus oldStatus, CartStatus newStatus, LocalDateTime cutoffDate) {
        String sql = """
            UPDATE carts
            SET status = ?, updated_at = ?
            WHERE status = ? AND updated_at < ?
            """;

        JdbcUtils.QueryResult result = jdbcUtils.executePreparedQuery(
                sql,
                newStatus.name(),
                Timestamp.valueOf(LocalDateTime.now()),
                oldStatus.name(),
                Timestamp.valueOf(cutoffDate)
        );

        return result.getAffectedRows();
    }

    @Override
    @Transactional
    public boolean deleteById(UUID id) {
        // First delete cart items
        deleteCartItemsByCartId(id);

        // Then delete the cart
        String sql = "DELETE FROM carts WHERE id = ?";
        JdbcUtils.QueryResult result = jdbcUtils.executePreparedQuery(sql, id);
        return result.getAffectedRows() > 0;
    }

    @Override
    public long count() {
        String sql = "SELECT COUNT(*) FROM carts";
        Long count = jdbcUtils.queryForObject(sql, Long.class);
        return count != null ? count : 0L;
    }

    @Override
    public boolean existsById(UUID id) {
        String sql = "SELECT COUNT(*) FROM carts WHERE id = ?";
        Long count = jdbcUtils.queryForObject(sql, Long.class, id);
        return count != null && count > 0;
    }

    @Override
    public List<Cart> findAll(int limit, int offset) {
        String sql = """
            SELECT id, date_created, updated_at, status, session_id, coupon_code, discount_amount
            FROM carts
            ORDER BY date_created DESC
            LIMIT ? OFFSET ?
            """;

        return jdbcUtils.query(sql, new CartRowMapper(), limit, offset);
    }

    // ========================================================================
    // CartItem CRUD Operations
    // ========================================================================

    @Override
    @Transactional
    public CartItem saveCartItem(CartItem cartItem) {
        String sql = """
            INSERT INTO cart_items (cart_id, product_id, quantity, unit_price, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?)
            """;

        LocalDateTime now = LocalDateTime.now();

        JdbcUtils.QueryResult result = jdbcUtils.executePreparedQuery(
                sql,
                cartItem.getCart().getId(),
                cartItem.getProduct().getId(),
                cartItem.getQuantity(),
                cartItem.getUnitPrice(),
                Timestamp.valueOf(now),
                Timestamp.valueOf(now)
        );

        if (result.hasError()) {
            logger.error("Error saving cart item: {}", result.getError());
            throw new RuntimeException("Failed to save cart item: " + result.getError());
        }

        if (result.getGeneratedKey() != null) {
            cartItem.setId(result.getGeneratedKey());
        }

        cartItem.setCreatedAt(now);
        cartItem.setUpdatedAt(now);

        return cartItem;
    }


    @Transactional
    public CartItem updateCartItem(CartItem cartItem) {
        String sql = """
            UPDATE cart_items
            SET quantity = ?, unit_price = ?, updated_at = ?
            WHERE id = ?
            """;

        LocalDateTime now = LocalDateTime.now();

        JdbcUtils.QueryResult result = jdbcUtils.executePreparedQuery(
                sql,
                cartItem.getQuantity(),
                cartItem.getUnitPrice(),
                Timestamp.valueOf(now),
                cartItem.getId()
        );

        if (result.hasError()) {
            logger.error("Error updating cart item: {}", result.getError());
            throw new RuntimeException("Failed to update cart item: " + result.getError());
        }

        cartItem.setUpdatedAt(now);
        return cartItem;
    }


    @Transactional
    public boolean deleteCartItemById(Long id) {
        String sql = "DELETE FROM cart_items WHERE id = ?";
        JdbcUtils.QueryResult result = jdbcUtils.executePreparedQuery(sql, id);
        return result.getAffectedRows() > 0;
    }

    @Override
    public List<CartItem> findCartItemsByCartId(UUID cartId) {
        String sql = """
            SELECT ci.id, ci.cart_id, ci.product_id, ci.quantity, ci.unit_price, ci.created_at, ci.updated_at,
                   p.id as p_id, p.name as p_name, p.description as p_description, p.price as p_price, 
                   p.stock_quantity as p_stock, p.sku as p_sku
            FROM cart_items ci
            INNER JOIN products p ON ci.product_id = p.id
            WHERE ci.cart_id = ?
            ORDER BY ci.created_at ASC
            """;

        return jdbcUtils.query(sql, new CartItemRowMapper(), cartId);
    }

    @Override
    @Transactional
    public int deleteCartItemsByCartId(UUID cartId) {
        String sql = "DELETE FROM cart_items WHERE cart_id = ?";
        JdbcUtils.QueryResult result = jdbcUtils.executePreparedQuery(sql, cartId);
        return result.getAffectedRows();
    }

    // ========================================================================
    // Row Mappers
    // ========================================================================

    /**
     * RowMapper for Cart entity
     */
    private static class CartRowMapper implements RowMapper<Cart> {
        @Override
        public Cart mapRow(ResultSet rs, int rowNum) throws SQLException {
            Cart cart = new Cart();
            cart.setId(UUID.fromString(rs.getString("id")));
            cart.setDateCreated(rs.getTimestamp("date_created").toLocalDateTime());

            Timestamp updatedAt = rs.getTimestamp("updated_at");
            if (updatedAt != null) {
                cart.setUpdatedAt(updatedAt.toLocalDateTime());
            }

            cart.setStatus(CartStatus.valueOf(rs.getString("status")));
            cart.setSessionId(rs.getString("session_id"));
            cart.setCouponCode(rs.getString("coupon_code"));

            BigDecimal discountAmount = rs.getBigDecimal("discount_amount");
            if (discountAmount != null) {
                cart.setDiscountAmount(discountAmount);
            }

            // Initialize empty items set
            cart.setItems(new LinkedHashSet<>());

            return cart;
        }
    }

    /**
     * RowMapper for CartItem entity
     */
    private static class CartItemRowMapper implements RowMapper<CartItem> {
        @Override
        public CartItem mapRow(ResultSet rs, int rowNum) throws SQLException {
            CartItem item = new CartItem();
            item.setId(rs.getLong("id"));
            item.setQuantity(rs.getInt("quantity"));
            item.setUnitPrice(rs.getBigDecimal("unit_price"));
            item.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());

            Timestamp updatedAt = rs.getTimestamp("updated_at");
            if (updatedAt != null) {
                item.setUpdatedAt(updatedAt.toLocalDateTime());
            }

            // Map Product (simplified - you may need to load full product separately)
            Product product = new Product();
            product.setId(rs.getLong("p_id"));
            product.setName(rs.getString("p_name"));
            product.setDescription(rs.getString("p_description"));
            product.setPrice(rs.getBigDecimal("p_price"));
            product.setStockQuantity(rs.getInt("p_stock"));
            product.setSku(rs.getString("p_sku"));

            item.setProduct(product);

            return item;
        }
    }
}