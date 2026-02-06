package com.smart_ecomernce_api.smart_ecomernce_api.modules.product.repository.impl;

import com.smart_ecomernce_api.smart_ecomernce_api.common.utils.JdbcUtils;
import com.smart_ecomernce_api.smart_ecomernce_api.common.utils.JdbcUtils.QueryResult;
import com.smart_ecomernce_api.smart_ecomernce_api.exception.InvalidDataException;
import com.smart_ecomernce_api.smart_ecomernce_api.exception.ResourceNotFoundException;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.category.entity.Category;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.product.entity.InventoryStatus;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.product.entity.Product;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.product.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.*;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * JDBC-based implementation of ProductRepository
 */
@Repository
public class ProductRepositoryImpl implements ProductRepository {
    private static final Logger logger = LoggerFactory.getLogger(ProductRepositoryImpl.class);

    private final JdbcUtils jdbcUtils;

    // Table and column names
    private static final String TABLE_NAME = "products";

    private static final String BASE_SELECT =
            "SELECT p.id, p.name, p.description, p.slug, p.sku, p.price, p.discount_price, p.cost_price, " +
                    "p.stock_quantity, p.reserved_quantity, p.low_stock_threshold, p.reorder_point, " +
                    "p.reorder_quantity, p.max_stock_quantity, p.inventory_status, p.track_inventory, " +
                    "p.allow_backorder, p.expected_restock_date, p.last_restocked_at, p.featured, p.is_new, " +
                    "p.is_bestseller, p.image_url, p.thumbnail_url, p.category_id, p.is_active, p.created_at, p.updated_at, " +
                    "(p.stock_quantity - p.reserved_quantity) as available_quantity, " +
                    "c.slug as category_slug, c.name as category_name " +
                    "FROM " + TABLE_NAME + " p " +
                    "LEFT JOIN categories c ON p.category_id = c.id";

    public ProductRepositoryImpl(JdbcUtils jdbcUtils) {
        this.jdbcUtils = jdbcUtils;
    }

    /**
     * RowMapper to convert ResultSet to Product entity
     */
    private final RowMapper<Product> productRowMapper = (rs, rowNum) -> {
        Product product = new Product();
        product.setId(rs.getLong("id"));
        product.setName(rs.getString("name"));
        product.setDescription(rs.getString("description"));
        product.setSlug(rs.getString("slug"));
        product.setSku(rs.getString("sku"));
        product.setPrice(rs.getBigDecimal("price") != null ? rs.getBigDecimal("price") : BigDecimal.ZERO);
        product.setDiscountPrice(rs.getBigDecimal("discount_price"));
        product.setCostPrice(rs.getBigDecimal("cost_price"));

        product.setStockQuantity(rs.getInt("stock_quantity"));
        product.setReservedQuantity(rs.getInt("reserved_quantity"));
        product.setLowStockThreshold(rs.getInt("low_stock_threshold"));
        product.setReorderPoint(rs.getInt("reorder_point"));

        Integer reorderQty = rs.getObject("reorder_quantity", Integer.class);
        product.setReorderQuantity(reorderQty);

        Integer maxStock = rs.getObject("max_stock_quantity", Integer.class);
        product.setMaxStockQuantity(maxStock);

        String statusStr = rs.getString("inventory_status");
        if (statusStr != null) {
            product.setInventoryStatus(InventoryStatus.valueOf(statusStr));
        }

        product.setTrackInventory(rs.getBoolean("track_inventory"));
        product.setAllowBackorder(rs.getBoolean("allow_backorder"));

        Timestamp expectedRestock = rs.getTimestamp("expected_restock_date");
        if (expectedRestock != null) {
            product.setExpectedRestockDate(expectedRestock.toLocalDateTime());
        }

        Timestamp lastRestocked = rs.getTimestamp("last_restocked_at");
        if (lastRestocked != null) {
            product.setLastRestockedAt(lastRestocked.toLocalDateTime());
        }

        product.setFeatured(rs.getBoolean("featured"));
        product.setIsNew(rs.getBoolean("is_new"));
        product.setIsBestseller(rs.getBoolean("is_bestseller"));
        product.setImageUrl(rs.getString("image_url"));
        product.setThumbnailUrl(rs.getString("thumbnail_url"));

        // Map category with full details from joined table
        Long categoryId = rs.getLong("category_id");
        if (categoryId != null && categoryId > 0) {
            Category category = new Category();
            category.setId(categoryId);
            category.setSlug(rs.getString("category_slug"));
            category.setName(rs.getString("category_name"));
            product.setCategory(category);
        }

        product.setIsActive(rs.getBoolean("is_active"));

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            product.setCreatedAt(createdAt.toLocalDateTime());
        }

        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            product.setUpdatedAt(updatedAt.toLocalDateTime());
        }

        return product;
    };

    // ==================== Basic CRUD Operations ====================

    @Override
    @Transactional
    public Product save(Product product) {
        if (product.getId() == null) {
            return insert(product);
        } else {
            return update(product);
        }
    }

    @Override
    @Transactional
    public List<Product> saveAll(Iterable<Product> products) {
        List<Product> result = new ArrayList<>();
        for (Product product : products) {
            result.add(save(product));
        }
        return result;
    }

    @Override
    public Optional<Product> findById(Long id) {
        String query = BASE_SELECT + " WHERE p.id = ?";
        List<Product> products = jdbcUtils.query(query, productRowMapper, id);
        return products.isEmpty() ? Optional.empty() : Optional.of(products.get(0));
    }

    @Override
    public boolean existsById(Long id) {
        String query = "SELECT COUNT(*) FROM " + TABLE_NAME + " WHERE id = ?";
        Long count = jdbcUtils.queryForObject(query, Long.class, id);
        return count != null && count > 0;
    }

    @Override
    public List<Product> findAll() {
        String query = BASE_SELECT;
        return jdbcUtils.query(query, productRowMapper);
    }

    @Override
    public List<Product> findAll(Sort sort) {
        String query = BASE_SELECT + buildOrderByClause(sort);
        return jdbcUtils.query(query, productRowMapper);
    }

    @Override
    public Page<Product> findAll(Pageable pageable) {
        long total = count();

        String query = BASE_SELECT +
                buildOrderByClause(pageable.getSort()) +
                " LIMIT ? OFFSET ?";

        List<Product> products = jdbcUtils.query(query, productRowMapper,
                pageable.getPageSize(), pageable.getOffset());

        return new PageImpl<>(products, pageable, total);
    }

    @Override
    public List<Product> findAllById(Iterable<Long> ids) {
        List<Long> idList = new ArrayList<>();
        ids.forEach(idList::add);

        if (idList.isEmpty()) {
            return Collections.emptyList();
        }

        String placeholders = idList.stream()
                .map(id -> "?")
                .collect(Collectors.joining(","));

        String query = BASE_SELECT + " WHERE p.id IN (" + placeholders + ")";
        return jdbcUtils.query(query, productRowMapper, idList.toArray());
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
        // First, delete associated product images
        String deleteProductImagesQuery = "DELETE FROM product_images WHERE product_id = ?";
        QueryResult imageResult = jdbcUtils.executePreparedQuery(deleteProductImagesQuery, id);

        if (imageResult.hasError()) {
            logger.error("Error deleting product images for product with id {}: {}", id, imageResult.getError());
            throw new InvalidDataException("Failed to delete product images for product with id " + id + ": " + imageResult.getError());
        }
        logger.info("Deleted {} product image(s) for product with id: {}", imageResult.getAffectedRows(), id);

        // Then, delete associated cart items to avoid foreign key constraints
        String deleteCartItemsQuery = "DELETE FROM cart_items WHERE product_id = ?";
        QueryResult cartResult = jdbcUtils.executePreparedQuery(deleteCartItemsQuery, id);

        if (cartResult.hasError()) {
            logger.error("Error deleting cart items for product with id {}: {}", id, cartResult.getError());
            throw new InvalidDataException("Failed to delete cart items for product with id " + id + ": " + cartResult.getError());
        }
        logger.info("Deleted {} cart item(s) for product with id: {}", cartResult.getAffectedRows(), id);

        // Then, delete associated wishlist items
        String deleteWishlistItemsQuery = "DELETE FROM wishlist_items WHERE product_id = ?";
        QueryResult wishlistResult = jdbcUtils.executePreparedQuery(deleteWishlistItemsQuery, id);

        if (wishlistResult.hasError()) {
            logger.error("Error deleting wishlist items for product with id {}: {}", id, wishlistResult.getError());
            throw new InvalidDataException("Failed to delete wishlist items for product with id " + id + ": " + wishlistResult.getError());
        }
        logger.info("Deleted {} wishlist item(s) for product with id: {}", wishlistResult.getAffectedRows(), id);


        String query = "DELETE FROM " + TABLE_NAME + " WHERE id = ?";
        QueryResult result = jdbcUtils.executePreparedQuery(query, id);

        if (result.hasError()) {
            logger.error("Error deleting product with id {}: {}", id, result.getError());
            throw new InvalidDataException("Failed to delete product with id " + id + ": " + result.getError());
        }

        if (result.getAffectedRows() <= 0) {
            logger.warn("Delete product with id {} did not affect any rows", id);
            throw new ResourceNotFoundException("Product not found with id: " + id);
        }

        logger.info("Deleted product with id: {}", id);
    }

    @Override
    @Transactional
    public void delete(Product product) {
        deleteById(product.getId());
    }

    @Override
    @Transactional
    public void deleteAllById(Iterable<Long> ids) {
        ids.forEach(this::deleteById);
    }

    @Override
    @Transactional
    public void deleteAll(Iterable<Product> products) {
        products.forEach(this::delete);
    }

    @Override
    @Transactional
    public void deleteAll() {
        String query = "DELETE FROM " + TABLE_NAME;
        jdbcUtils.executePreparedQuery(query);
        logger.warn("Deleted all products from database");
    }

    // ==================== Custom Query Methods ====================

    @Override
    public Optional<Product> findBySlug(String slug) {
        String query = BASE_SELECT + " WHERE p.slug = ?";
        List<Product> products = jdbcUtils.query(query, productRowMapper, slug);
        return products.isEmpty() ? Optional.empty() : Optional.of(products.get(0));
    }

    @Override
    public Optional<Product> findBySku(String sku) {
        String query = BASE_SELECT + " WHERE p.sku = ?";
        List<Product> products = jdbcUtils.query(query, productRowMapper, sku);
        return products.isEmpty() ? Optional.empty() : Optional.of(products.get(0));
    }

    @Override
    public boolean existsBySlug(String slug) {
        String query = "SELECT COUNT(*) FROM " + TABLE_NAME + " WHERE slug = ?";
        Long count = jdbcUtils.queryForObject(query, Long.class, slug);
        return count != null && count > 0;
    }

    @Override
    public boolean existsBySku(String sku) {
        String query = "SELECT COUNT(*) FROM " + TABLE_NAME + " WHERE sku = ?";
        Long count = jdbcUtils.queryForObject(query, Long.class, sku);
        return count != null && count > 0;
    }

    @Override
    public Optional<Product> findActiveById(Long id) {
        String query = BASE_SELECT + " WHERE p.is_active = true AND p.id = ?";
        List<Product> products = jdbcUtils.query(query, productRowMapper, id);
        return products.isEmpty() ? Optional.empty() : Optional.of(products.get(0));
    }

    @Override
    public Page<Product> findFeaturedProducts(Pageable pageable) {
        String countQuery = "SELECT COUNT(*) FROM " + TABLE_NAME + " p" +
                " WHERE p.featured = true AND p.is_active = true";
        Long total = jdbcUtils.queryForObject(countQuery, Long.class);
        long totalCount = total != null ? total : 0L;

        String query = BASE_SELECT + " WHERE p.featured = true AND p.is_active = true" +
                buildOrderByClause(pageable.getSort()) +
                " LIMIT ? OFFSET ?";

        List<Product> products = jdbcUtils.query(query, productRowMapper,
                pageable.getPageSize(), pageable.getOffset());

        return new PageImpl<>(products, pageable, totalCount);
    }

    @Override
    public Page<Product> findByCategory(Long categoryId, Pageable pageable) {
        String countQuery = "SELECT COUNT(*) FROM " + TABLE_NAME +
                " WHERE category_id = ? AND is_active = true";
        Long total = jdbcUtils.queryForObject(countQuery, Long.class, categoryId);
        long totalCount = total != null ? total : 0L;

        String query = BASE_SELECT + " WHERE p.category_id = ? AND p.is_active = true" +
                buildOrderByClause(pageable.getSort()) +
                " LIMIT ? OFFSET ?";

        List<Product> products = jdbcUtils.query(query, productRowMapper,
                categoryId, pageable.getPageSize(), pageable.getOffset());

        return new PageImpl<>(products, pageable, totalCount);
    }

    @Override
    public Page<Product> findByPriceRange(BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable) {
        Map<String, Object> params = new HashMap<>();
        params.put("minPrice", minPrice);
        params.put("maxPrice", maxPrice);

        String countQuery = "SELECT COUNT(*) FROM " + TABLE_NAME +
                " WHERE is_active = true AND discount_price >= :minPrice AND discount_price <= :maxPrice";
        Long total = jdbcUtils.queryForObject(countQuery, Long.class, params);
        long totalCount = total != null ? total : 0L;

        String query = BASE_SELECT +
                " WHERE p.is_active = true AND p.discount_price >= :minPrice AND p.discount_price <= :maxPrice" +
                buildOrderByClause(pageable.getSort()) +
                " LIMIT :limit OFFSET :offset";

        params.put("limit", pageable.getPageSize());
        params.put("offset", pageable.getOffset());

        List<Product> products = jdbcUtils.query(query, productRowMapper, params);

        return new PageImpl<>(products, pageable, totalCount);
    }

    @Override
    public Page<Product> searchByName(String search, Pageable pageable) {
        Map<String, Object> params = new HashMap<>();
        params.put("search", "%" + search.toLowerCase() + "%");

        String countQuery = "SELECT COUNT(*) FROM " + TABLE_NAME + " p" +
                " WHERE p.is_active = true AND LOWER(p.name) LIKE :search";
        Long total = jdbcUtils.queryForObject(countQuery, Long.class, params);
        long totalCount = total != null ? total : 0L;

        String query = BASE_SELECT +
                " WHERE p.is_active = true AND LOWER(p.name) LIKE :search" +
                buildOrderByClause(pageable.getSort()) +
                " LIMIT :limit OFFSET :offset";

        params.put("limit", pageable.getPageSize());
        params.put("offset", pageable.getOffset());

        List<Product> products = jdbcUtils.query(query, productRowMapper, params);

        return new PageImpl<>(products, pageable, totalCount);
    }

    @Override
    public Page<Product> searchProducts(String keyword, Pageable pageable) {
        Map<String, Object> params = new HashMap<>();
        params.put("keyword", "%" + keyword.toLowerCase() + "%");

        String countQuery = "SELECT COUNT(*) FROM " + TABLE_NAME + " p" +
                " WHERE p.is_active = true AND " +
                "(LOWER(p.name) LIKE :keyword OR LOWER(p.description) LIKE :keyword)";
        Long total = jdbcUtils.queryForObject(countQuery, Long.class, params);
        long totalCount = total != null ? total : 0L;

        String query = BASE_SELECT +
                " WHERE p.is_active = true AND " +
                "(LOWER(p.name) LIKE :keyword OR LOWER(p.description) LIKE :keyword)" +
                buildOrderByClause(pageable.getSort()) +
                " LIMIT :limit OFFSET :offset";

        params.put("limit", pageable.getPageSize());
        params.put("offset", pageable.getOffset());

        List<Product> products = jdbcUtils.query(query, productRowMapper, params);

        return new PageImpl<>(products, pageable, totalCount);
    }

    @Override
    public Page<Product> findByAdvancedFilters(Long categoryId, BigDecimal minPrice,
                                               BigDecimal maxPrice, String search, Pageable pageable) {
        Map<String, Object> params = new HashMap<>();
        params.put("categoryId", categoryId);
        params.put("minPrice", minPrice);
        params.put("maxPrice", maxPrice);
        params.put("search", "%" + search.toLowerCase() + "%");

        String countQuery = "SELECT COUNT(*) FROM " + TABLE_NAME + " p" +
                " WHERE p.is_active = true AND p.category_id = :categoryId " +
                "AND p.discount_price >= :minPrice AND p.discount_price <= :maxPrice " +
                "AND LOWER(p.name) LIKE :search";
        Long total = jdbcUtils.queryForObject(countQuery, Long.class, params);
        long totalCount = total != null ? total : 0L;

        String query = BASE_SELECT +
                " WHERE p.is_active = true AND p.category_id = :categoryId " +
                "AND p.discount_price >= :minPrice AND p.discount_price <= :maxPrice " +
                "AND LOWER(p.name) LIKE :search" +
                buildOrderByClause(pageable.getSort()) +
                " LIMIT :limit OFFSET :offset";

        params.put("limit", pageable.getPageSize());
        params.put("offset", pageable.getOffset());

        List<Product> products = jdbcUtils.query(query, productRowMapper, params);

        return new PageImpl<>(products, pageable, totalCount);
    }

    // ==================== Inventory Management Methods ====================

    @Override
    public List<Product> findByInventoryStatus(InventoryStatus status) {
        String query = BASE_SELECT + " WHERE inventory_status = ?";
        return jdbcUtils.query(query, productRowMapper, status.name());
    }

    @Override
    public Page<Product> findByInventoryStatus(InventoryStatus status, Pageable pageable) {
        Map<String, Object> params = new HashMap<>();
        params.put("status", status.name());

        String countQuery = "SELECT COUNT(*) FROM " + TABLE_NAME +
                " WHERE inventory_status = :status";
        Long total = jdbcUtils.queryForObject(countQuery, Long.class, params);
        long totalCount = total != null ? total : 0L;

        String query = BASE_SELECT +
                " WHERE p.inventory_status = :status" +
                buildOrderByClause(pageable.getSort()) +
                " LIMIT :limit OFFSET :offset";

        params.put("limit", pageable.getPageSize());
        params.put("offset", pageable.getOffset());

        List<Product> products = jdbcUtils.query(query, productRowMapper, params);
        return new PageImpl<>(products, pageable, totalCount);
    }

    @Override
    public List<Product> findLowStockProducts() {
        String query = BASE_SELECT +
                " WHERE inventory_status = 'LOW_STOCK' AND track_inventory = true AND is_active = true";
        return jdbcUtils.query(query, productRowMapper);
    }

    @Override
    public List<Product> findOutOfStockProducts() {
        String query = BASE_SELECT +
                " WHERE inventory_status = 'OUT_OF_STOCK' AND track_inventory = true AND is_active = true";
        return jdbcUtils.query(query, productRowMapper);
    }

    @Override
    public List<Product> findProductsNeedingReorder() {
        String query = BASE_SELECT +
                " WHERE stock_quantity <= reorder_point AND track_inventory = true AND is_active = true";
        return jdbcUtils.query(query, productRowMapper);
    }

    @Override
    public Page<Product> findProductsNeedingReorder(Pageable pageable) {
        String countQuery = "SELECT COUNT(*) FROM " + TABLE_NAME +
                " WHERE stock_quantity <= reorder_point AND track_inventory = true AND is_active = true";
        Long total = jdbcUtils.queryForObject(countQuery, Long.class);
        long totalCount = total != null ? total : 0L;

        Map<String, Object> params = new HashMap<>();
        String query = BASE_SELECT +
                " WHERE p.stock_quantity <= p.reorder_point AND p.track_inventory = true AND p.is_active = true" +
                buildOrderByClause(pageable.getSort()) +
                " LIMIT :limit OFFSET :offset";

        params.put("limit", pageable.getPageSize());
        params.put("offset", pageable.getOffset());

        List<Product> products = jdbcUtils.query(query, productRowMapper, params);
        return new PageImpl<>(products, pageable, totalCount);
    }

    @Override
    public Page<Product> findInStockProductsByCategory(Long categoryId, Pageable pageable) {
        Map<String, Object> params = new HashMap<>();
        params.put("categoryId", categoryId);

        String countQuery = "SELECT COUNT(*) FROM " + TABLE_NAME +
                " WHERE category_id = :categoryId AND inventory_status IN ('IN_STOCK', 'LOW_STOCK') AND is_active = true";
        Long total = jdbcUtils.queryForObject(countQuery, Long.class, params);
        long totalCount = total != null ? total : 0L;

        String query = BASE_SELECT +
                " WHERE p.category_id = :categoryId AND p.inventory_status IN ('IN_STOCK', 'LOW_STOCK') AND p.is_active = true" +
                buildOrderByClause(pageable.getSort()) +
                " LIMIT :limit OFFSET :offset";

        params.put("limit", pageable.getPageSize());
        params.put("offset", pageable.getOffset());

        List<Product> products = jdbcUtils.query(query, productRowMapper, params);

        return new PageImpl<>(products, pageable, totalCount);
    }

    @Override
    public List<Object[]> countByInventoryStatus() {
        String query = "SELECT inventory_status, COUNT(*) FROM " + TABLE_NAME +
                " WHERE track_inventory = true GROUP BY inventory_status";

        List<Map<String, Object>> results = jdbcUtils.executePreparedQuery(query).getResultSet();

        List<Object[]> countList = new ArrayList<>();
        for (Map<String, Object> row : results) {
            Object[] arr = new Object[2];
            arr[0] = row.get("inventory_status");
            arr[1] = row.get("COUNT(*)");
            countList.add(arr);
        }

        return countList;
    }

    @Override
    public List<Product> findByAvailableQuantityGreaterThan(Integer minQuantity) {
        String query = BASE_SELECT +
                " WHERE (stock_quantity - reserved_quantity) >= ? AND is_active = true";
        return jdbcUtils.query(query, productRowMapper, minQuantity);
    }

    @Override
    public boolean hasSufficientStock(Long productId, Integer quantity) {
        String query = "SELECT CASE WHEN (stock_quantity - reserved_quantity) >= ? " +
                "THEN 1 ELSE 0 END AS sufficient FROM " + TABLE_NAME + " WHERE id = ?";

        Integer result = jdbcUtils.queryForObject(query, Integer.class, quantity, productId);
        return result != null && result == 1;
    }

    @Override
    @Transactional
    public boolean updateStock(Long productId, Integer quantity) {
        Map<String, Object> params = new HashMap<>();
        params.put("quantity", quantity);
        params.put("updatedAt", Timestamp.valueOf(LocalDateTime.now()));
        params.put("id", productId);

        String query = "UPDATE " + TABLE_NAME +
                " SET stock_quantity = :quantity, updated_at = :updatedAt WHERE id = :id";

        QueryResult result = jdbcUtils.executeNamedQuery(query, params);
        return !result.hasError() && result.getAffectedRows() > 0;
    }

    @Override
    @Transactional
    public boolean reserveStock(Long productId, Integer quantity) {
        Map<String, Object> params = new HashMap<>();
        params.put("quantity", quantity);
        params.put("updatedAt", Timestamp.valueOf(LocalDateTime.now()));
        params.put("id", productId);

        String query = "UPDATE " + TABLE_NAME +
                " SET reserved_quantity = reserved_quantity + :quantity, updated_at = :updatedAt " +
                "WHERE id = :id AND (stock_quantity - reserved_quantity) >= :quantity";

        QueryResult result = jdbcUtils.executeNamedQuery(query, params);
        return !result.hasError() && result.getAffectedRows() > 0;
    }

    @Override
    @Transactional
    public boolean releaseReservedStock(Long productId, Integer quantity) {
        Map<String, Object> params = new HashMap<>();
        params.put("quantity", quantity);
        params.put("updatedAt", Timestamp.valueOf(LocalDateTime.now()));
        params.put("id", productId);

        String query = "UPDATE " + TABLE_NAME +
                " SET reserved_quantity = GREATEST(0, reserved_quantity - :quantity), updated_at = :updatedAt " +
                "WHERE id = :id";

        QueryResult result = jdbcUtils.executeNamedQuery(query, params);
        return !result.hasError() && result.getAffectedRows() > 0;
    }

    @Override
    @Transactional
    public boolean deductStock(Long productId, Integer quantity) {
        Map<String, Object> params = new HashMap<>();
        params.put("quantity", quantity);
        params.put("updatedAt", Timestamp.valueOf(LocalDateTime.now()));
        params.put("id", productId);

        String query = "UPDATE " + TABLE_NAME +
                " SET stock_quantity = stock_quantity - :quantity, " +
                "reserved_quantity = GREATEST(0, reserved_quantity - :quantity), " +
                "updated_at = :updatedAt " +
                "WHERE id = :id AND stock_quantity >= :quantity";

        QueryResult result = jdbcUtils.executeNamedQuery(query, params);
        return !result.hasError() && result.getAffectedRows() > 0;
    }

    @Override
    @Transactional
    public boolean updateInventoryStatus(Long productId, InventoryStatus status) {
        Map<String, Object> params = new HashMap<>();
        params.put("status", status.name());
        params.put("updatedAt", Timestamp.valueOf(LocalDateTime.now()));
        params.put("id", productId);

        String query = "UPDATE " + TABLE_NAME +
                " SET inventory_status = :status, updated_at = :updatedAt WHERE id = :id";

        QueryResult result = jdbcUtils.executeNamedQuery(query, params);
        return !result.hasError() && result.getAffectedRows() > 0;
    }

    @Override
    public List<Product> findProductsBelowReorderPoint() {
        String query = BASE_SELECT +
                " WHERE stock_quantity < reorder_point AND track_inventory = true AND is_active = true";
        return jdbcUtils.query(query, productRowMapper);
    }

    @Override
    public Page<Product> findNewProducts(Pageable pageable) {
        String countQuery = "SELECT COUNT(*) FROM " + TABLE_NAME +
                " WHERE is_new = true AND is_active = true";
        Long total = jdbcUtils.queryForObject(countQuery, Long.class);
        long totalCount = total != null ? total : 0L;

        String query = BASE_SELECT + " WHERE p.is_new = true AND p.is_active = true" +
                buildOrderByClause(pageable.getSort()) +
                " LIMIT ? OFFSET ?";

        List<Product> products = jdbcUtils.query(query, productRowMapper,
                pageable.getPageSize(), pageable.getOffset());

        return new PageImpl<>(products, pageable, totalCount);
    }

    @Override
    public Page<Product> findBestsellerProducts(Pageable pageable) {
        String countQuery = "SELECT COUNT(*) FROM " + TABLE_NAME +
                " WHERE is_bestseller = true AND is_active = true";
        Long total = jdbcUtils.queryForObject(countQuery, Long.class);
        long totalCount = total != null ? total : 0L;

        String query = BASE_SELECT + " WHERE p.is_bestseller = true AND p.is_active = true" +
                buildOrderByClause(pageable.getSort()) +
                " LIMIT ? OFFSET ?";

        List<Product> products = jdbcUtils.query(query, productRowMapper,
                pageable.getPageSize(), pageable.getOffset());

        return new PageImpl<>(products, pageable, totalCount);
    }

    @Override
    public Page<Product> findDiscountedProducts(Pageable pageable) {
        String countQuery = "SELECT COUNT(*) FROM " + TABLE_NAME +
                " WHERE discount_price IS NOT NULL AND discount_price < price AND is_active = true";
        Long total = jdbcUtils.queryForObject(countQuery, Long.class);
        long totalCount = total != null ? total : 0L;

        String query = BASE_SELECT +
                " WHERE p.discount_price IS NOT NULL AND p.discount_price < p.price AND p.is_active = true" +
                buildOrderByClause(pageable.getSort()) +
                " LIMIT ? OFFSET ?";

        List<Product> products = jdbcUtils.query(query, productRowMapper,
                pageable.getPageSize(), pageable.getOffset());

        return new PageImpl<>(products, pageable, totalCount);
    }

    @Override
    public Page<Product> findByCategories(List<Long> categoryIds, Pageable pageable) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            return Page.empty(pageable);
        }

        String placeholders = categoryIds.stream()
                .map(id -> "?")
                .collect(Collectors.joining(","));

        String countQuery = "SELECT COUNT(*) FROM " + TABLE_NAME +
                " WHERE category_id IN (" + placeholders + ") AND is_active = true";
        Long total = jdbcUtils.queryForObject(countQuery, Long.class, categoryIds.toArray());
        long totalCount = total != null ? total : 0L;

        String query = BASE_SELECT +
                " WHERE p.category_id IN (" + placeholders + ") AND p.is_active = true" +
                buildOrderByClause(pageable.getSort()) +
                " LIMIT ? OFFSET ?";

        List<Object> params = new ArrayList<>(categoryIds);
        params.add(pageable.getPageSize());
        params.add(pageable.getOffset());

        List<Product> products = jdbcUtils.query(query, productRowMapper, params.toArray());

        return new PageImpl<>(products, pageable, totalCount);
    }

    @Override
    @Transactional
    public boolean updatePrice(Long productId, BigDecimal price, BigDecimal discountPrice) {
        Map<String, Object> params = new HashMap<>();
        params.put("price", price);
        params.put("discountPrice", discountPrice);
        params.put("updatedAt", Timestamp.valueOf(LocalDateTime.now()));
        params.put("id", productId);

        String query = "UPDATE " + TABLE_NAME +
                " SET price = :price, discount_price = :discountPrice, updated_at = :updatedAt " +
                "WHERE id = :id";

        QueryResult result = jdbcUtils.executeNamedQuery(query, params);
        return !result.hasError() && result.getAffectedRows() > 0;
    }

    @Override
    public List<Product> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate) {
        String query = BASE_SELECT + " WHERE p.created_at BETWEEN ? AND ?";
        return jdbcUtils.query(query, productRowMapper,
                Timestamp.valueOf(startDate), Timestamp.valueOf(endDate));
    }

    @Override
    public Long countByCategory(Long categoryId) {
        String query = "SELECT COUNT(*) FROM " + TABLE_NAME + " p" +
                " WHERE p.category_id = ? AND p.is_active = true";
        Long count = jdbcUtils.queryForObject(query, Long.class, categoryId);
        return count != null ? count : 0L;
    }

    @Override
    @Transactional
    public int batchUpdate(List<Product> products) {
        if (products == null || products.isEmpty()) {
            return 0;
        }

        String query = "UPDATE " + TABLE_NAME +
                " SET name = :name, description = :description, slug = :slug, sku = :sku, " +
                "price = :price, discount_price = :discountPrice, stock_quantity = :stockQuantity, " +
                "inventory_status = :inventoryStatus, is_active = :isActive, updated_at = :updatedAt " +
                "WHERE id = :id";

        List<SqlParameterSource> batchParams = products.stream()
                .map(product -> {
                    product.setUpdatedAt(LocalDateTime.now());

                    MapSqlParameterSource params = new MapSqlParameterSource();
                    params.addValue("name", product.getName());
                    params.addValue("description", product.getDescription());
                    params.addValue("slug", product.getSlug());
                    params.addValue("sku", product.getSku());
                    params.addValue("price", product.getPrice());
                    params.addValue("discountPrice", product.getDiscountPrice());
                    params.addValue("stockQuantity", product.getStockQuantity());
                    params.addValue("inventoryStatus", product.getInventoryStatus().name());
                    params.addValue("isActive", product.getIsActive());
                    params.addValue("updatedAt", Timestamp.valueOf(product.getUpdatedAt()));
                    params.addValue("id", product.getId());

                    return params;
                })
                .collect(Collectors.toList());

        int[] results = jdbcUtils.batchUpdate(query, batchParams.toArray(new SqlParameterSource[0]));
        int totalUpdated = Arrays.stream(results).sum();

        logger.info("Batch updated {} products", totalUpdated);
        return totalUpdated;
    }

    // ==================== Helper Methods ====================

    @Transactional
    private Product insert(Product product) {
        LocalDateTime now = LocalDateTime.now();
        product.setCreatedAt(now);
        product.setUpdatedAt(now);

        if (product.getIsActive() == null) {
            product.setIsActive(true);
        }

        Map<String, Object> params = buildProductParams(product);

        String query = "INSERT INTO " + TABLE_NAME +
                " (name, description, slug, sku, price, discount_price, cost_price, " +
                "stock_quantity, reserved_quantity, low_stock_threshold, reorder_point, " +
                "reorder_quantity, max_stock_quantity, inventory_status, track_inventory, " +
                "allow_backorder, expected_restock_date, last_restocked_at, featured, is_new, " +
                "is_bestseller, image_url, thumbnail_url, category_id, is_active, created_at, updated_at) " +
                "VALUES (:name, :description, :slug, :sku, :price, :discountPrice, :costPrice, " +
                ":stockQuantity, :reservedQuantity, :lowStockThreshold, :reorderPoint, " +
                ":reorderQuantity, :maxStockQuantity, :inventoryStatus, :trackInventory, " +
                ":allowBackorder, :expectedRestockDate, :lastRestockedAt, :featured, :isNew, " +
                ":isBestseller, :imageUrl, :thumbnailUrl, :categoryId, :isActive, :createdAt, :updatedAt)";

        QueryResult result = jdbcUtils.executeNamedQuery(query, params);

        if (result.getGeneratedKey() != null) {
            product.setId(result.getGeneratedKey());
        }

        logger.info("Inserted product with id: {}", product.getId());
        return product;
    }

    @Transactional
    private Product update(Product product) {
        product.setUpdatedAt(LocalDateTime.now());

        Map<String, Object> params = buildProductParams(product);
        params.put("id", product.getId());

        String query = "UPDATE " + TABLE_NAME +
                " SET name = :name, description = :description, slug = :slug, sku = :sku, " +
                "price = :price, discount_price = :discountPrice, cost_price = :costPrice, " +
                "stock_quantity = :stockQuantity, reserved_quantity = :reservedQuantity, " +
                "low_stock_threshold = :lowStockThreshold, reorder_point = :reorderPoint, " +
                "reorder_quantity = :reorderQuantity, max_stock_quantity = :maxStockQuantity, " +
                "inventory_status = :inventoryStatus, track_inventory = :trackInventory, " +
                "allow_backorder = :allowBackorder, expected_restock_date = :expectedRestockDate, " +
                "last_restocked_at = :lastRestockedAt, featured = :featured, is_new = :isNew, " +
                "is_bestseller = :isBestseller, image_url = :imageUrl, thumbnail_url = :thumbnailUrl, " +
                "category_id = :categoryId, is_active = :isActive, updated_at = :updatedAt " +
                "WHERE id = :id";

        jdbcUtils.executeNamedQuery(query, params);
        logger.info("Updated product with id: {}", product.getId());

        return product;
    }

    private Map<String, Object> buildProductParams(Product product) {
        Map<String, Object> params = new HashMap<>();
        params.put("name", product.getName());
        params.put("description", product.getDescription());
        params.put("slug", product.getSlug());
        params.put("sku", product.getSku());
        params.put("price", product.getPrice());
        params.put("discountPrice", product.getDiscountPrice());
        params.put("costPrice", product.getCostPrice());
        params.put("stockQuantity", product.getStockQuantity());
        params.put("reservedQuantity", product.getReservedQuantity());
        params.put("lowStockThreshold", product.getLowStockThreshold());
        params.put("reorderPoint", product.getReorderPoint());
        params.put("reorderQuantity", product.getReorderQuantity());
        params.put("maxStockQuantity", product.getMaxStockQuantity());
        params.put("inventoryStatus", product.getInventoryStatus().name());
        params.put("trackInventory", product.getTrackInventory());
        params.put("allowBackorder", product.getAllowBackorder());

        params.put("expectedRestockDate",
                product.getExpectedRestockDate() != null ?
                        Timestamp.valueOf(product.getExpectedRestockDate()) : null);

        params.put("lastRestockedAt",
                product.getLastRestockedAt() != null ?
                        Timestamp.valueOf(product.getLastRestockedAt()) : null);

        params.put("featured", product.getFeatured());
        params.put("isNew", product.getIsNew());
        params.put("isBestseller", product.getIsBestseller());
        params.put("imageUrl", product.getImageUrl());
        params.put("thumbnailUrl", product.getThumbnailUrl());

        // Note: Category should be set as category_id
        // You'll need to extract the ID from the Category object
        params.put("categoryId", product.getCategory() != null ? product.getCategory().getId() : null);

        params.put("isActive", product.getIsActive());
        params.put("createdAt", Timestamp.valueOf(product.getCreatedAt()));
        params.put("updatedAt", Timestamp.valueOf(product.getUpdatedAt()));

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
            orderBy.append("p.").append(camelToSnake(order.getProperty()))
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
