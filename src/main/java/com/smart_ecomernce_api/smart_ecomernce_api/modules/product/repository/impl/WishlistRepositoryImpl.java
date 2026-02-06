package com.smart_ecomernce_api.smart_ecomernce_api.modules.product.repository.impl;

import com.smart_ecomernce_api.smart_ecomernce_api.common.utils.JdbcUtils;
import com.smart_ecomernce_api.smart_ecomernce_api.common.utils.JdbcUtils.QueryResult;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.product.entity.Product;
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
        // Do not call non-existent setters like setUserId/setProductId on entity

        // Build Product if product columns are present
        try {
            // product_id should always be present
            Long prodId = rs.getLong("product_id");
            Product product = new Product();
            product.setId(prodId);

            // optional product columns (available when joined)
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
                java.math.BigDecimal discountPrice = rs.getBigDecimal("discount_price");
                if (discountPrice != null) product.setDiscountPrice(discountPrice);
            } catch (Exception ignored) {}
            try {
                String imageUrl = rs.getString("image_url");
                if (imageUrl != null) product.setImageUrl(imageUrl);
            } catch (Exception ignored) {}
            try {
                String inv = rs.getString("inventory_status");
                if (inv != null) {
                    try {
                        // InventoryStatus enum exists on Product
                        product.setInventoryStatus(com.smart_ecomernce_api.smart_ecomernce_api.modules.product.entity.InventoryStatus.valueOf(inv));
                    } catch (IllegalArgumentException ignored) {}
                }
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

            item.setProduct(product);
        } catch (Exception ex) {
            // If product_id isn't available or any other issue, leave product null
        }

        // Map wishlist item fields
        item.setNotes(rs.getString("notes"));

        String priorityStr = rs.getString("priority");
        if (priorityStr != null) {
            try {
                item.setPriority(WishlistPriority.valueOf(priorityStr));
            } catch (IllegalArgumentException ignored) {}
        }

        item.setDesiredQuantity(rs.getObject("desired_quantity") != null ? rs.getInt("desired_quantity") : 1);
        item.setPriceWhenAdded(rs.getBigDecimal("price_when_added"));
        item.setNotifyOnPriceDrop(rs.getObject("notify_on_price_drop") != null && rs.getBoolean("notify_on_price_drop"));
        item.setTargetPrice(rs.getBigDecimal("target_price"));
        item.setNotifyOnStock(rs.getObject("notify_on_stock") != null && rs.getBoolean("notify_on_stock"));
        item.setPurchased(rs.getObject("purchased") != null && rs.getBoolean("purchased"));

        Timestamp purchasedAt = null;
        try { purchasedAt = rs.getTimestamp("purchased_at"); } catch (Exception ignored) {}
        if (purchasedAt != null) item.setPurchasedAt(purchasedAt.toLocalDateTime());

        item.setIsPublic(rs.getObject("is_public") != null && rs.getBoolean("is_public"));
        item.setIsActive(rs.getObject("is_active") != null && rs.getBoolean("is_active"));

        Timestamp createdAt = null;
        try { createdAt = rs.getTimestamp("created_at"); } catch (Exception ignored) {}
        if (createdAt != null) item.setCreatedAt(createdAt.toLocalDateTime());

        Timestamp updatedAt = null;
        try { updatedAt = rs.getTimestamp("updated_at"); } catch (Exception ignored) {}
        if (updatedAt != null) item.setUpdatedAt(updatedAt.toLocalDateTime());

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
        // Join products so the mapper can populate Product details and avoid nulls
        String query = "SELECT w.*, p.id AS product_id, p.name AS product_name, p.slug AS product_slug, p.price, p.discount_price, p.image_url, p.inventory_status, p.stock_quantity, p.reserved_quantity, (p.stock_quantity - p.reserved_quantity) AS available_quantity " +
                "FROM wishlist_items w INNER JOIN products p ON w.product_id = p.id " +
                "WHERE w.user_id = ? ORDER BY w.created_at DESC";
        return jdbcUtils.query(query, wishlistItemRowMapper, userId);
    }

    @Override
    public Page<WishlistItem> findByUserId(Long userId, Pageable pageable) {
        String countQuery = "SELECT COUNT(*) FROM " + TABLE_NAME + " WHERE user_id = ?";
        Long total = jdbcUtils.queryForObject(countQuery, Long.class, userId);
        long totalCount = total != null ? total : 0L;

        String query = "SELECT w.*, p.id AS product_id, p.name AS product_name, p.slug AS product_slug, p.price, p.discount_price, p.image_url, p.inventory_status, p.stock_quantity, p.reserved_quantity, (p.stock_quantity - p.reserved_quantity) AS available_quantity " +
                "FROM wishlist_items w INNER JOIN products p ON w.product_id = p.id " +
                "WHERE w.user_id = ? " +
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
    public List<WishlistItem> findItemsBelowTargetPrice(Long userId) {
        String query = BASE_SELECT + " w " +
                "INNER JOIN products p ON w.product_id = p.id " +
                "WHERE w.user_id = ? AND w.target_price > p.discount_price AND w.purchased = false";
        return jdbcUtils.query(query, wishlistItemRowMapper, userId);
    }

    @Override
    public List<WishlistItem> findItemsForPriceUpdate() {
        String query = BASE_SELECT + " WHERE notify_on_price_drop = true OR notify_on_stock = true";
        return jdbcUtils.query(query, wishlistItemRowMapper);
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
  public void deleteByUserIdAndProductId(Long userId, Long productId) {
      String query = "DELETE FROM " + TABLE_NAME + " WHERE user_id = ? AND product_id = ?";
      QueryResult result = jdbcUtils.executePreparedQuery(query, userId, productId);
      if (!result.hasError()) {
          logger.info("Deleted wishlist item for user id: {} and product id: {}", userId, productId);
      }
  }

    @Override
    public List<WishlistItem> findByUserIdAndPriority(Long userId, WishlistPriority priority) {
        String query = BASE_SELECT + " WHERE user_id = ? AND priority = ?";
        return jdbcUtils.query(query, wishlistItemRowMapper, userId, priority.name());
    }

    @Override
    public List<WishlistItem> findByUserIdAndPriorityOrderByCreatedAtDesc(Long userId, WishlistPriority priority) {
        String query = BASE_SELECT + " WHERE user_id = ? AND priority = ? ORDER BY created_at DESC";
        return jdbcUtils.query(query, wishlistItemRowMapper, userId, priority.name());
    }

    @Override
    public List<WishlistItem> findByUserIdAndCollectionName(Long userId, String collectionName) {
        String query = BASE_SELECT + " WHERE user_id = ? AND collection_name = ?";
        return jdbcUtils.query(query, wishlistItemRowMapper, userId, collectionName);
    }

    @Override
    public List<String> findDistinctCollectionsByUserId(Long userId) {
        String query = "SELECT DISTINCT collection_name FROM " + TABLE_NAME + " WHERE user_id = ? AND collection_name IS NOT NULL";
        List<String> result = new ArrayList<>();
        jdbcUtils.query(query, (rs, rowNum) -> {
            result.add(rs.getString("collection_name"));
            return null;
        }, userId);
        return result;
    }

    @Override
    public List<Object[]> findCategoryAnalyticsByUserId(Long userId) {
        String query = "SELECT category_id, COUNT(*) FROM " + TABLE_NAME + " WHERE user_id = ? GROUP BY category_id";
        List<Object[]> result = new ArrayList<>();
        jdbcUtils.query(query, (rs, rowNum) -> {
            result.add(new Object[]{rs.getObject(1), rs.getObject(2)});
            return null;
        }, userId);
        return result;
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
    public List<WishlistItem> findByUserIdAndTagsContaining(Long userId, String tag) {
        String query = BASE_SELECT + " WHERE user_id = ? AND tags LIKE ?";
        return jdbcUtils.query(query, wishlistItemRowMapper, userId, "%" + tag + "%");
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
    public List<WishlistItem> findPurchasedByUserIdAndDateRange(Long userId, LocalDateTime startDate, LocalDateTime endDate) {
        String query = BASE_SELECT + " WHERE user_id = ? AND purchased = true AND purchased_at BETWEEN ? AND ? ORDER BY purchased_at DESC";
        return jdbcUtils.query(query, wishlistItemRowMapper, userId, Timestamp.valueOf(startDate), Timestamp.valueOf(endDate));
    }

    @Override
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
    public List<WishlistItem> findByGuestSessionIdOrderByCreatedAtDesc(String guestSessionId) {
        String query = BASE_SELECT + " WHERE guest_session_id = ? ORDER BY created_at DESC";
        return jdbcUtils.query(query, wishlistItemRowMapper, guestSessionId);
    }

    @Override
    public Optional<WishlistItem> findByGuestSessionIdAndProductId(String guestSessionId, Long productId) {
        String query = BASE_SELECT + " WHERE guest_session_id = ? AND product_id = ?";
        List<WishlistItem> items = jdbcUtils.query(query, wishlistItemRowMapper, guestSessionId, productId);
        return items.isEmpty() ? Optional.empty() : Optional.of(items.get(0));
    }

    @Override
    public boolean existsByGuestSessionIdAndProductId(String guestSessionId, Long productId) {
        String query = "SELECT COUNT(*) FROM " + TABLE_NAME + " WHERE guest_session_id = ? AND product_id = ?";
        Long count = jdbcUtils.queryForObject(query, Long.class, guestSessionId, productId);
        return count != null && count > 0;
    }

    @Override
    public void deleteByGuestSessionId(String guestSessionId) {
        String query = "DELETE FROM " + TABLE_NAME + " WHERE guest_session_id = ?";
        jdbcUtils.executePreparedQuery(query, guestSessionId);
    }

    @Override
    public void deleteByGuestSessionIdAndProductId(String guestSessionId, Long productId) {
        String query = "DELETE FROM " + TABLE_NAME + " WHERE guest_session_id = ? AND product_id = ?";
        jdbcUtils.executePreparedQuery(query, guestSessionId, productId);
    }

    @Override
    public Long countByGuestSessionId(String guestSessionId) {
        String query = "SELECT COUNT(*) FROM " + TABLE_NAME + " WHERE guest_session_id = ?";
        Long count = jdbcUtils.queryForObject(query, Long.class, guestSessionId);
        return count != null ? count : 0L;
    }

    @Override
    public List<WishlistItem> findExpiredGuestSessions(LocalDateTime currentTime) {
        String query = BASE_SELECT + " WHERE guest_session_id IS NOT NULL AND updated_at < ?";
        return jdbcUtils.query(query, wishlistItemRowMapper, Timestamp.valueOf(currentTime));
    }

    @Override
    public void deleteExpiredGuestSessions(LocalDateTime currentTime) {
        String query = "DELETE FROM " + TABLE_NAME + " WHERE guest_session_id IS NOT NULL AND updated_at < ?";
        jdbcUtils.executePreparedQuery(query, Timestamp.valueOf(currentTime));
    }

    @Override
    public List<WishlistItem> findPublicItemsByUserIdOrderByCreatedAtDesc(Long userId) {
        String query = BASE_SELECT + " WHERE user_id = ? AND is_public = true ORDER BY created_at DESC";
        return jdbcUtils.query(query, wishlistItemRowMapper, userId);
    }

    @Override
    public void updatePublicStatus(Long userId, boolean isPublic) {
        String query = "UPDATE " + TABLE_NAME + " SET is_public = ? WHERE user_id = ?";
        jdbcUtils.executePreparedQuery(query, isPublic, userId);
    }

    @Override
    public void updatePublicStatusForItems(List<Long> itemIds, boolean isPublic) {
        if (itemIds == null || itemIds.isEmpty()) return;
        String placeholders = itemIds.stream().map(id -> "?").collect(Collectors.joining(","));
        String query = "UPDATE " + TABLE_NAME + " SET is_public = ? WHERE id IN (" + placeholders + ")";
        List<Object> params = new ArrayList<>();
        params.add(isPublic);
        params.addAll(itemIds);
        jdbcUtils.executePreparedQuery(query, params.toArray());
    }

    @Override
    public List<WishlistItem> findItemsWithDueReminders(LocalDateTime currentTime) {
        String query = BASE_SELECT + " WHERE reminder_enabled = true AND reminder_date <= ?";
        return jdbcUtils.query(query, wishlistItemRowMapper, Timestamp.valueOf(currentTime));
    }

    @Override
    public List<WishlistItem> findByUserIdAndReminderEnabled(Long userId, boolean reminderEnabled) {
        String query = BASE_SELECT + " WHERE user_id = ? AND reminder_enabled = ?";
        return jdbcUtils.query(query, wishlistItemRowMapper, userId, reminderEnabled);
    }

    @Override
    public void updateReminder(Long itemId, LocalDateTime reminderDate, boolean reminderEnabled) {
        String query = "UPDATE " + TABLE_NAME + " SET reminder_date = ?, reminder_enabled = ? WHERE id = ?";
        jdbcUtils.executePreparedQuery(query, Timestamp.valueOf(reminderDate), reminderEnabled, itemId);
    }

    @Override
    public void cancelReminder(Long itemId) {
        String query = "UPDATE " + TABLE_NAME + " SET reminder_enabled = false, reminder_date = NULL WHERE id = ?";
        jdbcUtils.executePreparedQuery(query, itemId);
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
    public void markMultipleAsPurchased(List<Long> wishlistItemIds) {
        if (wishlistItemIds == null || wishlistItemIds.isEmpty()) return;
        String placeholders = wishlistItemIds.stream().map(id -> "?").collect(Collectors.joining(","));
        String query = "UPDATE " + TABLE_NAME + " SET purchased = true, purchased_at = ?, updated_at = ? WHERE id IN (" + placeholders + ")";
        List<Object> params = new ArrayList<>();
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        params.add(now);
        params.add(now);
        params.addAll(wishlistItemIds);
        jdbcUtils.executePreparedQuery(query, params.toArray());
    }

    @Override
    public List<WishlistItem> findPublicItemsByUserId(Long userId) {
        String query = BASE_SELECT + " WHERE user_id = ? AND is_public = true ORDER BY created_at DESC";
        return jdbcUtils.query(query, wishlistItemRowMapper, userId);
    }

    @Override
    public void updateCollection(List<Long> itemIds, String collectionName) {
        if (itemIds == null || itemIds.isEmpty()) return;
        String placeholders = itemIds.stream().map(id -> "?").collect(Collectors.joining(","));
        String query = "UPDATE " + TABLE_NAME + " SET collection_name = ? WHERE id IN (" + placeholders + ")";
        List<Object> params = new ArrayList<>();
        params.add(collectionName);
        params.addAll(itemIds);
        jdbcUtils.executePreparedQuery(query, params.toArray());
    }

    @Override
    public void updateTags(Long itemId, String tags) {
        String query = "UPDATE " + TABLE_NAME + " SET tags = ? WHERE id = ?";
        jdbcUtils.executePreparedQuery(query, tags, itemId);
    }

    @Override
    public List<WishlistItem> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate) {
        String query = BASE_SELECT + " WHERE created_at BETWEEN ? AND ?";
        return jdbcUtils.query(query, wishlistItemRowMapper, Timestamp.valueOf(startDate), Timestamp.valueOf(endDate));
    }

    @Override
    public List<WishlistItem> findByUserIdAndCreatedAtBetween(Long userId, LocalDateTime startDate, LocalDateTime endDate) {
        String query = BASE_SELECT + " WHERE user_id = ? AND created_at BETWEEN ? AND ?";
        return jdbcUtils.query(query, wishlistItemRowMapper, userId, Timestamp.valueOf(startDate), Timestamp.valueOf(endDate));
    }

    @Override
    public Long countByUserIdAndCreatedAtAfter(Long userId, LocalDateTime date) {
        String query = "SELECT COUNT(*) FROM " + TABLE_NAME + " WHERE user_id = ? AND created_at > ?";
        Long count = jdbcUtils.queryForObject(query, Long.class, userId, Timestamp.valueOf(date));
        return count != null ? count : 0L;
    }

    @Override
    public Double calculateAveragePriceDropByUserId(Long userId) {
        String query = "SELECT AVG(price_when_added - target_price) FROM " + TABLE_NAME + " WHERE user_id = ? AND price_when_added IS NOT NULL AND target_price IS NOT NULL";
        Double avg = jdbcUtils.queryForObject(query, Double.class, userId);
        return avg != null ? avg : 0.0;
    }

    @Override
    public Double calculateTotalSavingsByUserId(Long userId) {
        String query = "SELECT SUM(price_when_added - target_price) FROM " + TABLE_NAME + " WHERE user_id = ? AND price_when_added IS NOT NULL AND target_price IS NOT NULL";
        Double sum = jdbcUtils.queryForObject(query, Double.class, userId);
        return sum != null ? sum : 0.0;
    }

    @Override
    public void transferGuestItemsToUser(String guestSessionId, Long userId) {
        String query = "UPDATE " + TABLE_NAME + " SET user_id = ?, guest_session_id = NULL WHERE guest_session_id = ?";
        jdbcUtils.executePreparedQuery(query, userId, guestSessionId);
    }

    @Override
    public void mergeGuestItemsToUser(String guestSessionId, Long userId) {
        // This is a placeholder for more complex logic, e.g., merging duplicates
        transferGuestItemsToUser(guestSessionId, userId);
    }

    @Override
    public List<WishlistItem> findByUserIdAndProductIdIn(Long userId, List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) return Collections.emptyList();
        String placeholders = productIds.stream().map(id -> "?").collect(Collectors.joining(","));
        String query = BASE_SELECT + " WHERE user_id = ? AND product_id IN (" + placeholders + ")";
        List<Object> params = new ArrayList<>();
        params.add(userId);
        params.addAll(productIds);
        return jdbcUtils.query(query, wishlistItemRowMapper, params.toArray());
    }

    @Override
    public void deleteByUserIdAndProductIdIn(Long userId, List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) return;
        String placeholders = productIds.stream().map(id -> "?").collect(Collectors.joining(","));
        String query = "DELETE FROM " + TABLE_NAME + " WHERE user_id = ? AND product_id IN (" + placeholders + ")";
        List<Object> params = new ArrayList<>();
        params.add(userId);
        params.addAll(productIds);
        jdbcUtils.executePreparedQuery(query, params.toArray());
    }

    @Override
    public int bulkUpdatePriority(List<Long> itemIds, WishlistPriority priority) {
        if (itemIds == null || itemIds.isEmpty()) return 0;
        String placeholders = itemIds.stream().map(id -> "?").collect(Collectors.joining(","));
        String query = "UPDATE " + TABLE_NAME + " SET priority = ? WHERE id IN (" + placeholders + ")";
        List<Object> params = new ArrayList<>();
        params.add(priority.name());
        params.addAll(itemIds);
        QueryResult result = jdbcUtils.executePreparedQuery(query, params.toArray());
        return result.getAffectedRows();
    }

    @Override
    public int bulkUpdateCollection(Long userId, List<Long> productIds, String collectionName) {
        if (productIds == null || productIds.isEmpty()) return 0;
        String placeholders = productIds.stream().map(id -> "?").collect(Collectors.joining(","));
        String query = "UPDATE " + TABLE_NAME + " SET collection_name = ? WHERE user_id = ? AND product_id IN (" + placeholders + ")";
        List<Object> params = new ArrayList<>();
        params.add(collectionName);
        params.add(userId);
        params.addAll(productIds);
        QueryResult result = jdbcUtils.executePreparedQuery(query, params.toArray());
        return result.getAffectedRows();
    }

    @Override
    public List<WishlistItem> searchByUserIdAndKeyword(Long userId, String keyword) {
        String query = BASE_SELECT + " WHERE user_id = ? AND (notes LIKE ? OR tags LIKE ?)";
        String like = "%" + keyword + "%";
        return jdbcUtils.query(query, wishlistItemRowMapper, userId, like, like);
    }

    @Override
    public Page<WishlistItem> findByUserIdWithFilters(Long userId, WishlistPriority priority, Boolean purchased, Boolean inStock, String collectionName, Pageable pageable) {
        StringBuilder query = new StringBuilder(BASE_SELECT + " WHERE user_id = ?");
        List<Object> params = new ArrayList<>();
        params.add(userId);
        if (priority != null) {
            query.append(" AND priority = ?");
            params.add(priority.name());
        }
        if (purchased != null) {
            query.append(" AND purchased = ?");
            params.add(purchased);
        }
        if (inStock != null) {
            query.append(" AND in_stock = ?");
            params.add(inStock);
        }
        if (collectionName != null && !collectionName.isEmpty()) {
            query.append(" AND collection_name = ?");
            params.add(collectionName);
        }
        long total = jdbcUtils.queryForObject("SELECT COUNT(*) FROM (" + query + ") t", Long.class, params.toArray());
        query.append(buildOrderByClause(pageable.getSort()));
        query.append(" LIMIT ? OFFSET ?");
        params.add(pageable.getPageSize());
        params.add(pageable.getOffset());
        List<WishlistItem> items = jdbcUtils.query(query.toString(), wishlistItemRowMapper, params.toArray());
        return new PageImpl<>(items, pageable, total);
    }

    @Override
    public List<WishlistItem> findByProductIdIn(List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) return Collections.emptyList();
        String placeholders = productIds.stream().map(id -> "?").collect(Collectors.joining(","));
        String query = BASE_SELECT + " WHERE product_id IN (" + placeholders + ")";
        return jdbcUtils.query(query, wishlistItemRowMapper, productIds.toArray());
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

    @Override
    public List<Long> findAllUserIdsWithWishlists() {
        String query = "SELECT DISTINCT user_id FROM " + TABLE_NAME + " WHERE user_id IS NOT NULL";
        List<Long> result = new ArrayList<>();
        jdbcUtils.query(query, (rs, rowNum) -> {
            result.add(rs.getLong("user_id"));
            return null;
        });
        return result;
    }
}

