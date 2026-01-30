package com.smart_ecomernce_api.smart_ecomernce_api.modules.product.repository.impl;

import com.smart_ecomernce_api.smart_ecomernce_api.common.utils.JdbcUtils;
import com.smart_ecomernce_api.smart_ecomernce_api.common.utils.JdbcUtils.QueryResult;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.product.entity.WishlistItem;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.product.entity.WishlistPriority;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.product.repository.WishlistRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.*;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * JDBC-based implementation of WishlistRepository
 */
@Repository
public class WishlistRepositoryImpl implements WishlistRepository {
    private static final Logger logger = LoggerFactory.getLogger(WishlistRepositoryImpl.class);

    private final JdbcUtils jdbcUtils;

    // Table and column names
    private static final String TABLE_NAME = "wishlist_items";

    private static final String BASE_SELECT =
            "SELECT id, user_id, product_id, notes, priority, desired_quantity, " +
                    "price_when_added, notify_on_price_drop, target_price, notify_on_stock, " +
                    "purchased, purchased_at, is_public, is_active, created_at, updated_at " +
                    "FROM " + TABLE_NAME;

    public WishlistRepositoryImpl(JdbcUtils jdbcUtils) {
        this.jdbcUtils = jdbcUtils;
    }

    /**
     * RowMapper to convert ResultSet to WishlistItem entity
     */
    private final RowMapper<WishlistItem> wishlistItemRowMapper = (rs, rowNum) -> {
        WishlistItem item = new WishlistItem();
        item.setId(rs.getLong("id"));

        // Note: User and Product are set as IDs only
        // You'll need to load full objects separately if needed
        // For now, we'll skip setting the full objects to avoid circular dependencies

        item.setNotes(rs.getString("notes"));

        String priorityStr = rs.getString("priority");
        if (priorityStr != null) {
            item.setPriority(WishlistPriority.valueOf(priorityStr));
        }

        item.setDesiredQuantity(rs.getInt("desired_quantity"));
        item.setPriceWhenAdded(rs.getBigDecimal("price_when_added"));
        item.setNotifyOnPriceDrop(rs.getBoolean("notify_on_price_drop"));
        item.setTargetPrice(rs.getBigDecimal("target_price"));
        item.setNotifyOnStock(rs.getBoolean("notify_on_stock"));
        item.setPurchased(rs.getBoolean("purchased"));

        Timestamp purchasedAt = rs.getTimestamp("purchased_at");
        if (purchasedAt != null) {
            item.setPurchasedAt(purchasedAt.toLocalDateTime());
        }

        item.setIsPublic(rs.getBoolean("is_public"));
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

    // ==================== Basic CRUD Operations ====================

    @Override
    @Transactional
    public WishlistItem save(WishlistItem item) {
        if (item.getId() == null) {
            return insert(item);
        } else {
            return update(item);
        }
    }

    @Override
    @Transactional
    public List<WishlistItem> saveAll(Iterable<WishlistItem> items) {
        List<WishlistItem> result = new ArrayList<>();
        for (WishlistItem item : items) {
            result.add(save(item));
        }
        return result;
    }

    @Override
    public Optional<WishlistItem> findById(Long id) {
        String query = BASE_SELECT + " WHERE id = ?";
        List<WishlistItem> items = jdbcUtils.query(query, wishlistItemRowMapper, id);
        return items.isEmpty() ? Optional.empty() : Optional.of(items.get(0));
    }

    @Override
    public boolean existsById(Long id) {
        String query = "SELECT COUNT(*) FROM " + TABLE_NAME + " WHERE id = ?";
        Long count = jdbcUtils.queryForObject(query, Long.class, id);
        return count != null && count > 0;
    }

    @Override
    public List<WishlistItem> findAll() {
        return jdbcUtils.query(BASE_SELECT, wishlistItemRowMapper);
    }

    @Override
    public List<WishlistItem> findAll(Sort sort) {
        String query = BASE_SELECT + buildOrderByClause(sort);
        return jdbcUtils.query(query, wishlistItemRowMapper);
    }

    @Override
    public Page<WishlistItem> findAll(Pageable pageable) {
        long total = count();

        String query = BASE_SELECT +
                buildOrderByClause(pageable.getSort()) +
                " LIMIT ? OFFSET ?";

        List<WishlistItem> items = jdbcUtils.query(query, wishlistItemRowMapper,
                pageable.getPageSize(), pageable.getOffset());

        return new PageImpl<>(items, pageable, total);
    }

    @Override
    public List<WishlistItem> findAllById(Iterable<Long> ids) {
        List<Long> idList = new ArrayList<>();
        ids.forEach(idList::add);

        if (idList.isEmpty()) {
            return Collections.emptyList();
        }

        String placeholders = idList.stream()
                .map(id -> "?")
                .collect(Collectors.joining(","));

        String query = BASE_SELECT + " WHERE id IN (" + placeholders + ")";
        return jdbcUtils.query(query, wishlistItemRowMapper, idList.toArray());
    }

    @Override
    public long count() {
        String query = "SELECT COUNT(*) FROM " + TABLE_NAME;
        Long count = jdbcUtils.queryForObject(query, Long.class);
        return count != null ? count : 0L;
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        String query = "DELETE FROM " + TABLE_NAME + " WHERE id = ?";
        QueryResult result = jdbcUtils.executePreparedQuery(query, id);
        if (!result.hasError()) {
            logger.info("Deleted wishlist item with id: {}", id);
        }
    }

    @Override
    @Transactional
    public void delete(WishlistItem item) {
        deleteById(item.getId());
    }

    @Override
    @Transactional
    public void deleteAllById(Iterable<Long> ids) {
        ids.forEach(this::deleteById);
    }

    @Override
    @Transactional
    public void deleteAll(Iterable<WishlistItem> items) {
        items.forEach(this::delete);
    }

    @Override
    @Transactional
    public void deleteAll() {
        String query = "DELETE FROM " + TABLE_NAME;
        jdbcUtils.executePreparedQuery(query);
        logger.warn("Deleted all wishlist items from database");
    }

    // ==================== Custom Query Methods ====================

    @Override
    public List<WishlistItem> findByUserIdOrderByCreatedAtDesc(Long userId) {
        String query = BASE_SELECT + " WHERE user_id = ? ORDER BY created_at DESC";
        return jdbcUtils.query(query, wishlistItemRowMapper, userId);
    }

    @Override
    public Page<WishlistItem> findByUserId(Long userId, Pageable pageable) {
        String countQuery = "SELECT COUNT(*) FROM " + TABLE_NAME + " WHERE user_id = ?";
        Long total = jdbcUtils.queryForObject(countQuery, Long.class, userId);
        long totalCount = total != null ? total : 0L;

        String query = BASE_SELECT + " WHERE user_id = ?" +
                buildOrderByClause(pageable.getSort()) +
                " LIMIT ? OFFSET ?";

        List<WishlistItem> items = jdbcUtils.query(query, wishlistItemRowMapper,
                userId, pageable.getPageSize(), pageable.getOffset());

        return new PageImpl<>(items, pageable, totalCount);
    }

    @Override
    public Optional<WishlistItem> findByUserIdAndProductId(Long userId, Long productId) {
        String query = BASE_SELECT + " WHERE user_id = ? AND product_id = ?";
        List<WishlistItem> items = jdbcUtils.query(query, wishlistItemRowMapper, userId, productId);
        return items.isEmpty() ? Optional.empty() : Optional.of(items.get(0));
    }

    @Override
    public boolean existsByUserIdAndProductId(Long userId, Long productId) {
        String query = "SELECT COUNT(*) FROM " + TABLE_NAME + " WHERE user_id = ? AND product_id = ?";
        Long count = jdbcUtils.queryForObject(query, Long.class, userId, productId);
        return count != null && count > 0;
    }

    @Override
    public List<WishlistItem> findItemsWithPriceDrops(Long userId) {
        String query = BASE_SELECT + " w " +
                "INNER JOIN products p ON w.product_id = p.id " +
                "WHERE w.user_id = ? " +
                "AND w.price_when_added > p.discount_price " +
                "AND w.purchased = false " +
                "ORDER BY (w.price_when_added - p.discount_price) DESC";

        return jdbcUtils.query(query, wishlistItemRowMapper, userId);
    }

    @Override
    public List<WishlistItem> findItemsNeedingStockNotification(Long userId) {
        String query = BASE_SELECT + " w " +
                "INNER JOIN products p ON w.product_id = p.id " +
                "WHERE w.user_id = ? " +
                "AND w.notify_on_stock = true " +
                "AND w.purchased = false " +
                "AND p.inventory_status IN ('IN_STOCK', 'LOW_STOCK')";

        return jdbcUtils.query(query, wishlistItemRowMapper, userId);
    }

    @Override
    public List<WishlistItem> findItemsNeedingPriceNotification(Long userId) {
        String query = BASE_SELECT + " w " +
                "INNER JOIN products p ON w.product_id = p.id " +
                "WHERE w.user_id = ? " +
                "AND w.notify_on_price_drop = true " +
                "AND w.target_price >= p.discount_price " +
                "AND w.purchased = false";

        return jdbcUtils.query(query, wishlistItemRowMapper, userId);
    }

    @Override
    @Transactional
    public void deleteByUserId(Long userId) {
        String query = "DELETE FROM " + TABLE_NAME + " WHERE user_id = ?";
        QueryResult result = jdbcUtils.executePreparedQuery(query, userId);
        if (!result.hasError()) {
            logger.info("Deleted all wishlist items for user id: {}", userId);
        }
    }

    @Override
    public List<WishlistItem> findByUserIdAndPriority(Long userId, WishlistPriority priority) {
        String query = BASE_SELECT + " WHERE user_id = ? AND priority = ?";
        return jdbcUtils.query(query, wishlistItemRowMapper, userId, priority.name());
    }

    @Override
    public List<WishlistItem> findUnpurchasedByUserId(Long userId) {
        String query = BASE_SELECT + " WHERE user_id = ? AND purchased = false ORDER BY created_at DESC";
        return jdbcUtils.query(query, wishlistItemRowMapper, userId);
    }

    @Override
    public List<WishlistItem> findPurchasedByUserId(Long userId) {
        String query = BASE_SELECT + " WHERE user_id = ? AND purchased = true ORDER BY purchased_at DESC";
        return jdbcUtils.query(query, wishlistItemRowMapper, userId);
    }

    @Override
    public Long countByUserId(Long userId) {
        String query = "SELECT COUNT(*) FROM " + TABLE_NAME + " WHERE user_id = ?";
        Long count = jdbcUtils.queryForObject(query, Long.class, userId);
        return count != null ? count : 0L;
    }

    @Override
    public Long countUnpurchasedByUserId(Long userId) {
        String query = "SELECT COUNT(*) FROM " + TABLE_NAME + " WHERE user_id = ? AND purchased = false";
        Long count = jdbcUtils.queryForObject(query, Long.class, userId);
        return count != null ? count : 0L;
    }

    @Override
    public List<WishlistItem> findPublicItemsByUserId(Long userId) {
        String query = BASE_SELECT + " WHERE user_id = ? AND is_public = true ORDER BY created_at DESC";
        return jdbcUtils.query(query, wishlistItemRowMapper, userId);
    }

    @Override
    @Transactional
    public boolean markAsPurchased(Long wishlistItemId) {
        Map<String, Object> params = new HashMap<>();
        params.put("purchased", true);
        params.put("purchasedAt", Timestamp.valueOf(LocalDateTime.now()));
        params.put("updatedAt", Timestamp.valueOf(LocalDateTime.now()));
        params.put("id", wishlistItemId);

        String query = "UPDATE " + TABLE_NAME +
                " SET purchased = :purchased, purchased_at = :purchasedAt, updated_at = :updatedAt " +
                "WHERE id = :id";

        QueryResult result = jdbcUtils.executeNamedQuery(query, params);
        return !result.hasError() && result.getAffectedRows() > 0;
    }

    @Override
    @Transactional
    public boolean updateNotes(Long wishlistItemId, String notes) {
        Map<String, Object> params = new HashMap<>();
        params.put("notes", notes);
        params.put("updatedAt", Timestamp.valueOf(LocalDateTime.now()));
        params.put("id", wishlistItemId);

        String query = "UPDATE " + TABLE_NAME +
                " SET notes = :notes, updated_at = :updatedAt WHERE id = :id";

        QueryResult result = jdbcUtils.executeNamedQuery(query, params);
        return !result.hasError() && result.getAffectedRows() > 0;
    }

    @Override
    @Transactional
    public boolean updatePriority(Long wishlistItemId, WishlistPriority priority) {
        Map<String, Object> params = new HashMap<>();
        params.put("priority", priority.name());
        params.put("updatedAt", Timestamp.valueOf(LocalDateTime.now()));
        params.put("id", wishlistItemId);

        String query = "UPDATE " + TABLE_NAME +
                " SET priority = :priority, updated_at = :updatedAt WHERE id = :id";

        QueryResult result = jdbcUtils.executeNamedQuery(query, params);
        return !result.hasError() && result.getAffectedRows() > 0;
    }

    @Override
    @Transactional
    public boolean updatePriceNotificationSettings(Long wishlistItemId, boolean notifyOnPriceDrop, BigDecimal targetPrice) {
        Map<String, Object> params = new HashMap<>();
        params.put("notifyOnPriceDrop", notifyOnPriceDrop);
        params.put("targetPrice", targetPrice);
        params.put("updatedAt", Timestamp.valueOf(LocalDateTime.now()));
        params.put("id", wishlistItemId);

        String query = "UPDATE " + TABLE_NAME +
                " SET notify_on_price_drop = :notifyOnPriceDrop, target_price = :targetPrice, " +
                "updated_at = :updatedAt WHERE id = :id";

        QueryResult result = jdbcUtils.executeNamedQuery(query, params);
        return !result.hasError() && result.getAffectedRows() > 0;
    }

    @Override
    @Transactional
    public boolean updateStockNotificationSetting(Long wishlistItemId, boolean notifyOnStock) {
        Map<String, Object> params = new HashMap<>();
        params.put("notifyOnStock", notifyOnStock);
        params.put("updatedAt", Timestamp.valueOf(LocalDateTime.now()));
        params.put("id", wishlistItemId);

        String query = "UPDATE " + TABLE_NAME +
                " SET notify_on_stock = :notifyOnStock, updated_at = :updatedAt WHERE id = :id";

        QueryResult result = jdbcUtils.executeNamedQuery(query, params);
        return !result.hasError() && result.getAffectedRows() > 0;
    }

    @Override
    public List<WishlistItem> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate) {
        String query = BASE_SELECT + " WHERE created_at BETWEEN ? AND ?";
        return jdbcUtils.query(query, wishlistItemRowMapper,
                Timestamp.valueOf(startDate), Timestamp.valueOf(endDate));
    }

    @Override
    public List<WishlistItem> findByProductId(Long productId) {
        String query = BASE_SELECT + " WHERE product_id = ?";
        return jdbcUtils.query(query, wishlistItemRowMapper, productId);
    }

    @Override
    public Long countByProductId(Long productId) {
        String query = "SELECT COUNT(*) FROM " + TABLE_NAME + " WHERE product_id = ?";
        Long count = jdbcUtils.queryForObject(query, Long.class, productId);
        return count != null ? count : 0L;
    }

    @Override
    @Transactional
    public void deleteByUserIdAndProductId(Long userId, Long productId) {
        String query = "DELETE FROM " + TABLE_NAME + " WHERE user_id = ? AND product_id = ?";
        QueryResult result = jdbcUtils.executePreparedQuery(query, userId, productId);
        if (!result.hasError()) {
            logger.info("Deleted wishlist item for user {} and product {}", userId, productId);
        }
    }

    // ==================== Helper Methods ====================

    @Transactional
    private WishlistItem insert(WishlistItem item) {
        LocalDateTime now = LocalDateTime.now();
        item.setCreatedAt(now);
        item.setUpdatedAt(now);

        if (item.getIsActive() == null) {
            item.setIsActive(true);
        }

        Map<String, Object> params = buildWishlistItemParams(item);

        String query = "INSERT INTO " + TABLE_NAME +
                " (user_id, product_id, notes, priority, desired_quantity, price_when_added, " +
                "notify_on_price_drop, target_price, notify_on_stock, purchased, purchased_at, " +
                "is_public, is_active, created_at, updated_at) " +
                "VALUES (:userId, :productId, :notes, :priority, :desiredQuantity, :priceWhenAdded, " +
                ":notifyOnPriceDrop, :targetPrice, :notifyOnStock, :purchased, :purchasedAt, " +
                ":isPublic, :isActive, :createdAt, :updatedAt)";

        QueryResult result = jdbcUtils.executeNamedQuery(query, params);

        if (result.getGeneratedKey() != null) {
            item.setId(result.getGeneratedKey());
        }

        logger.info("Inserted wishlist item with id: {}", item.getId());
        return item;
    }

    @Transactional
    private WishlistItem update(WishlistItem item) {
        item.setUpdatedAt(LocalDateTime.now());

        Map<String, Object> params = buildWishlistItemParams(item);
        params.put("id", item.getId());

        String query = "UPDATE " + TABLE_NAME +
                " SET user_id = :userId, product_id = :productId, notes = :notes, priority = :priority, " +
                "desired_quantity = :desiredQuantity, price_when_added = :priceWhenAdded, " +
                "notify_on_price_drop = :notifyOnPriceDrop, target_price = :targetPrice, " +
                "notify_on_stock = :notifyOnStock, purchased = :purchased, purchased_at = :purchasedAt, " +
                "is_public = :isPublic, is_active = :isActive, updated_at = :updatedAt " +
                "WHERE id = :id";

        jdbcUtils.executeNamedQuery(query, params);
        logger.info("Updated wishlist item with id: {}", item.getId());

        return item;
    }

    private Map<String, Object> buildWishlistItemParams(WishlistItem item) {
        Map<String, Object> params = new HashMap<>();

        // Extract user_id and product_id from objects
        // Note: You'll need to handle this based on how your entities are structured
        params.put("userId", item.getUser() != null ? item.getUser().getId() : null);
        params.put("productId", item.getProduct() != null ? item.getProduct().getId() : null);

        params.put("notes", item.getNotes());
        params.put("priority", item.getPriority() != null ? item.getPriority().name() : WishlistPriority.MEDIUM.name());
        params.put("desiredQuantity", item.getDesiredQuantity());
        params.put("priceWhenAdded", item.getPriceWhenAdded());
        params.put("notifyOnPriceDrop", item.getNotifyOnPriceDrop());
        params.put("targetPrice", item.getTargetPrice());
        params.put("notifyOnStock", item.getNotifyOnStock());
        params.put("purchased", item.getPurchased());

        params.put("purchasedAt",
                item.getPurchasedAt() != null ?
                        Timestamp.valueOf(item.getPurchasedAt()) : null);

        params.put("isPublic", item.getIsPublic());
        params.put("isActive", item.getIsActive());
        params.put("createdAt", Timestamp.valueOf(item.getCreatedAt()));
        params.put("updatedAt", Timestamp.valueOf(item.getUpdatedAt()));

        return params;
    }

    private String buildOrderByClause(Sort sort) {
        if (sort == null || !sort.iterator().hasNext()) {
            return "";
        }

        StringBuilder orderBy = new StringBuilder(" ORDER BY ");
        Iterator<Sort.Order> iterator = sort.iterator();

        while (iterator.hasNext()) {
            Sort.Order order = iterator.next();
            orderBy.append(camelToSnake(order.getProperty()))
                    .append(" ")
                    .append(order.getDirection().name());

            if (iterator.hasNext()) {
                orderBy.append(", ");
            }
        }

        return orderBy.toString();
    }

    private String camelToSnake(String camelCase) {
        return camelCase.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }
}