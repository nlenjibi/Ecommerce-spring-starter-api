package com.smart_ecomernce_api.smart_ecomernce_api.modules.cart.repository.impl;

import com.smart_ecomernce_api.smart_ecomernce_api.common.utils.JdbcUtils;
import com.smart_ecomernce_api.smart_ecomernce_api.common.utils.JdbcUtils.QueryResult;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.cart.entity.Cart;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.cart.entity.CartItem;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.cart.entity.CartStatus;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.cart.repository.CartRepository;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.product.entity.Product;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.user.entity.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.RowMapper;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;

/**
 * JDBC-based implementation of CartRepository
 * Uses JdbcUtils for all database operations
 */
@Repository
public class CartRepositoryImpl implements CartRepository {
    private static final Logger logger = LoggerFactory.getLogger(CartRepositoryImpl.class);

    private final JdbcUtils jdbcUtils;

    @PersistenceContext
    private EntityManager entityManager;

    // Table and column names for Cart
    private static final String CART_TABLE = "carts";
    private static final String CART_ITEM_TABLE = "cart_items";

    // Base SELECT queries
    private static final String CART_BASE_SELECT =
            "SELECT id, session_id, status, user_id, coupon_code, discount_amount, " +
                    "is_active, created_at, updated_at FROM " + CART_TABLE;

    private static final String CART_ITEM_BASE_SELECT =
            "SELECT id, cart_id, product_id, quantity, total_price, " +
                    "is_active, created_at, updated_at FROM " + CART_ITEM_TABLE;

    public CartRepositoryImpl(JdbcUtils jdbcUtils) {
        this.jdbcUtils = jdbcUtils;
    }

    /**
     * RowMapper to convert ResultSet to Cart entity
     */
    private final RowMapper<Cart> cartRowMapper = (rs, rowNum) -> {
        Cart cart = new Cart();
        cart.setId(rs.getLong("id"));


        String statusStr = rs.getString("status");
        if (statusStr != null) {
            cart.setStatus(CartStatus.valueOf(statusStr));
        }

        Long userId = rs.getObject("user_id", Long.class);
        if (userId != null) {
            User user = new User();
            user.setId(userId);
            cart.setUser(user);
        }

        cart.setCouponCode(rs.getString("coupon_code"));
        cart.setDiscountAmount(rs.getBigDecimal("discount_amount"));
        cart.setIsActive(rs.getBoolean("is_active"));

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            cart.setCreatedAt(createdAt.toLocalDateTime());
        }

        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            cart.setUpdatedAt(updatedAt.toLocalDateTime());
        }

        return cart;
    };

    /**
     * RowMapper to convert ResultSet to CartItem entity
     */
    private final RowMapper<CartItem> cartItemRowMapper = (rs, rowNum) -> {
        CartItem item = new CartItem();
        item.setId(rs.getLong("id"));

        Long cartId = rs.getObject("cart_id", Long.class);
        if (cartId != null) {
            Cart cart = new Cart();
            cart.setId(cartId);
            item.setCart(cart);
        }

        Long productId = rs.getObject("product_id", Long.class);
        if (productId != null) {
            Product product = new Product();
            product.setId(productId);

            // Populate product fields if available from joined query
            try {
                String name = rs.getString("product_name");
                if (name != null) product.setName(name);
            } catch (Exception ignored) {}
            try {
                String slug = rs.getString("product_slug");
                if (slug != null) product.setSlug(slug);
            } catch (Exception ignored) {}
            try {
                java.math.BigDecimal price = rs.getBigDecimal("price");
                if (price != null) product.setPrice(price);
            } catch (Exception ignored) {}
            try {
                java.math.BigDecimal discount = rs.getBigDecimal("discount_price");
                if (discount != null) product.setDiscountPrice(discount);
            } catch (Exception ignored) {}
            try {
                String sku = rs.getString("sku");
                if (sku != null) product.setSku(sku);
            } catch (Exception ignored) {}
            try {
                Integer stockQty = rs.getObject("stock_quantity") != null ? rs.getInt("stock_quantity") : null;
                if (stockQty != null) product.setStockQuantity(stockQty);
            } catch (Exception ignored) {}
            try {
                Integer reservedQty = rs.getObject("reserved_quantity") != null ? rs.getInt("reserved_quantity") : null;
                if (reservedQty != null) product.setReservedQuantity(reservedQty);
            } catch (Exception ignored) {}
            try {
                Integer avail = rs.getObject("available_quantity") != null ? rs.getInt("available_quantity") : null;
                if (avail != null) product.setAvailableQuantity(avail);
            } catch (Exception ignored) {}
            try {
                String imageUrl = rs.getString("image_url");
                if (imageUrl != null) product.setImageUrl(imageUrl);
            } catch (Exception ignored) {}
            try {
                Boolean featured = rs.getObject("featured") != null ? rs.getBoolean("featured") : null;
                if (featured != null) product.setFeatured(featured);
            } catch (Exception ignored) {}
            try {
                String inv = rs.getString("inventory_status");
                if (inv != null) {
                    try {
                        product.setInventoryStatus(com.smart_ecomernce_api.smart_ecomernce_api.modules.product.entity.InventoryStatus.valueOf(inv));
                    } catch (IllegalArgumentException ignored) {}
                }
            } catch (Exception ignored) {}
            try {
                Long categoryId = rs.getObject("category_id", Long.class);
                if (categoryId != null) {
                    // set only id to avoid loading full Category here
                    com.smart_ecomernce_api.smart_ecomernce_api.modules.category.entity.Category cat = new com.smart_ecomernce_api.smart_ecomernce_api.modules.category.entity.Category();
                    cat.setId(categoryId);
                    product.setCategory(cat);
                }
            } catch (Exception ignored) {}

            item.setProduct(product);
        }

        item.setQuantity(rs.getInt("quantity"));
        item.setTotalPrice(rs.getBigDecimal("total_price"));
        item.setIsActive(rs.getBoolean("is_active"));

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            item.setCreatedAt(createdAt.toLocalDateTime());
        }

        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            item.setUpdatedAt(updatedAt.toLocalDateTime());
        }

        return item;
    };

    // ==================== Cart CRUD Operations ====================

    @Override
    @Transactional
    public Cart save(Cart cart) {
        if (cart.getId() == null) {
            entityManager.persist(cart);
            return cart;
        } else {
            return entityManager.merge(cart);
        }
    }

    @Override
    @Transactional
    public Cart update(Cart cart) {
        return entityManager.merge(cart);
    }

    @Override
    public Optional<Cart> findById(Long id) {
        String query = CART_BASE_SELECT + " WHERE id = ?";
        List<Cart> carts = jdbcUtils.query(query, cartRowMapper, id);
        return carts.isEmpty() ? Optional.empty() : Optional.of(carts.get(0));
    }

    @Override
    public Optional<Cart> findByIdWithItems(Long id) {
        Optional<Cart> cartOpt = findById(id);
        if (cartOpt.isEmpty()) {
            return Optional.empty();
        }

        Cart cart = cartOpt.get();
        List<CartItem> items = findCartItemsByCartId(id);
        cart.setItems(new HashSet<>(items));

        return Optional.of(cart);
    }

    @Override
    public Optional<Cart> findActiveCartByUserId(Long userId) {
        String query = CART_BASE_SELECT + " WHERE user_id = ? AND status = ? AND is_active = true";
        List<Cart> carts = jdbcUtils.query(query, cartRowMapper, userId, CartStatus.ACTIVE.name());

        if (carts.isEmpty()) {
            return Optional.empty();
        }

        Cart cart = carts.get(0);
        List<CartItem> items = findCartItemsByCartId(cart.getId());
        cart.setItems(new HashSet<>(items));

        return Optional.of(cart);
    }

    @Override
    public List<Cart> findAbandonedCartsBefore(LocalDateTime cutoffDate) {
        String query = CART_BASE_SELECT +
                " WHERE status = ? AND updated_at < ? AND is_active = true";
        return jdbcUtils.query(query, cartRowMapper,
                CartStatus.ACTIVE.name(), Timestamp.valueOf(cutoffDate));
    }

    @Override
    public List<Cart> findEmptyCartsBefore(LocalDateTime cutoffDate) {
        String query = "SELECT c.* FROM " + CART_TABLE + " c " +
                "LEFT JOIN " + CART_ITEM_TABLE + " ci ON c.id = ci.cart_id " +
                "WHERE c.updated_at < ? AND c.is_active = true " +
                "GROUP BY c.id " +
                "HAVING COUNT(ci.id) = 0";

        return jdbcUtils.query(query, cartRowMapper, Timestamp.valueOf(cutoffDate));
    }

    @Override
    @Transactional
    public boolean deleteById(Long id) {
        // First delete all cart items
        deleteCartItemsByCartId(id);

        // Then delete the cart
        String query = "DELETE FROM " + CART_TABLE + " WHERE id = ?";
        QueryResult result = jdbcUtils.executePreparedQuery(query, id);

        if (!result.hasError() && result.getAffectedRows() > 0) {
            logger.info("Deleted cart with id: {}", id);
            return true;
        }
        return false;
    }

    @Override
    public long count() {
        String query = "SELECT COUNT(*) FROM " + CART_TABLE;
        Long count = jdbcUtils.queryForObject(query, Long.class);
        return count != null ? count : 0L;
    }

    @Override
    public boolean existsById(Long id) {
        String query = "SELECT COUNT(*) FROM " + CART_TABLE + " WHERE id = ?";
        Long count = jdbcUtils.queryForObject(query, Long.class, id);
        return count != null && count > 0;
    }

    @Override
    public List<Cart> findAll(int limit, int offset) {
        String query = CART_BASE_SELECT + " LIMIT ? OFFSET ?";
        return jdbcUtils.query(query, cartRowMapper, limit, offset);
    }

    @Override
    public List<Cart> findAll() {
        return jdbcUtils.query(CART_BASE_SELECT, cartRowMapper);
    }

    // ==================== CartItem CRUD Operations ====================

    @Override
    @Transactional
    public CartItem saveCartItem(CartItem cartItem) {
        if (cartItem.getId() == null) {
            entityManager.persist(cartItem);
            return cartItem;
        } else {
            return entityManager.merge(cartItem);
        }
    }

    @Override
    public List<CartItem> findCartItemsByCartId(Long cartId) {
        String query = "SELECT ci.*, " +
                "p.id AS product_id, p.name AS product_name, p.slug AS product_slug, p.price, p.discount_price, p.sku, p.stock_quantity, p.reserved_quantity, (p.stock_quantity - p.reserved_quantity) AS available_quantity, p.image_url, p.featured, p.inventory_status, p.category_id " +
                "FROM " + CART_ITEM_TABLE + " ci LEFT JOIN products p ON ci.product_id = p.id " +
                "WHERE ci.cart_id = ?";
        return jdbcUtils.query(query, cartItemRowMapper, cartId);
    }

    @Override
    @Transactional
    public int deleteCartItemsByCartId(Long cartId) {
        String query = "DELETE FROM " + CART_ITEM_TABLE + " WHERE cart_id = ?";
        QueryResult result = jdbcUtils.executePreparedQuery(query, cartId);

        int affectedRows = result.getAffectedRows();
        if (!result.hasError()) {
            logger.info("Deleted {} cart items for cart id: {}", affectedRows, cartId);
        }
        return affectedRows;
    }
}
