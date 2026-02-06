package com.smart_ecomernce_api.smart_ecomernce_api.modules.product.repository;

import com.smart_ecomernce_api.smart_ecomernce_api.modules.product.entity.InventoryStatus;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Custom ProductRepository interface for JDBC-based implementation
 * No JPA dependencies - pure JDBC operations
 */
public interface ProductRepository {

    // ==================== Basic CRUD Operations ====================

    /**
     * Save a product (insert if new, update if existing)
     */
    Product save(Product product);

    /**
     * Save multiple products
     */
    List<Product> saveAll(Iterable<Product> products);

    /**
     * Find product by ID
     */
    Optional<Product> findById(Long id);

    /**
     * Check if product exists by ID
     */
    boolean existsById(Long id);

    /**
     * Find all products
     */
    List<Product> findAll();

    /**
     * Find all products with sorting
     */
    List<Product> findAll(Sort sort);

    /**
     * Find all products with pagination
     */
    Page<Product> findAll(Pageable pageable);

    /**
     * Find all products by IDs
     */
    List<Product> findAllById(Iterable<Long> ids);

    /**
     * Count total number of products
     */
    long count();

    /**
     * Delete product by ID
     */
    void deleteById(Long id);

    /**
     * Delete a product
     */
    void delete(Product product);

    /**
     * Delete products by IDs
     */
    void deleteAllById(Iterable<Long> ids);

    /**
     * Delete multiple products
     */
    void deleteAll(Iterable<Product> products);

    /**
     * Delete all products
     */
    void deleteAll();

    // ==================== Custom Query Methods ====================

    /**
     * Find product by slug
     */
    Optional<Product> findBySlug(String slug);

    /**
     * Find product by SKU
     */
    Optional<Product> findBySku(String sku);

    /**
     * Check if slug exists
     */
    boolean existsBySlug(String slug);

    /**
     * Check if SKU exists
     */
    boolean existsBySku(String sku);

    /**
     * Find active product by ID
     */
    Optional<Product> findActiveById(Long id);

    /**
     * Find featured and active products with pagination
     */
    Page<Product> findFeaturedProducts(Pageable pageable);

    /**
     * Find products by category
     */
    Page<Product> findByCategory(Long categoryId, Pageable pageable);

    /**
     * Find products by price range
     */
    Page<Product> findByPriceRange(BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);

    /**
     * Search products by name
     */
    Page<Product> searchByName(String search, Pageable pageable);

    /**
     * Search products by keyword (name or description)
     */
    Page<Product> searchProducts(String keyword, Pageable pageable);

    /**
     * Find products with advanced filters
     */
    Page<Product> findByAdvancedFilters(Long categoryId, BigDecimal minPrice,
                                        BigDecimal maxPrice, String search, Pageable pageable);

    // ==================== Inventory Management Methods ====================

    /**
     * Find products by inventory status
     */
    List<Product> findByInventoryStatus(InventoryStatus status);

    /**
     * Find products by inventory status with pagination
     */
    Page<Product> findByInventoryStatus(InventoryStatus status, Pageable pageable);

    /**
     * Find low stock products
     */
    List<Product> findLowStockProducts();

    /**
     * Find out of stock products
     */
    List<Product> findOutOfStockProducts();

    /**
     * Find products needing reorder
     */
    List<Product> findProductsNeedingReorder();

    /**
     * Find products needing reorder with pagination
     */
    Page<Product> findProductsNeedingReorder(Pageable pageable);

    /**
     * Find in-stock products by category
     */
    Page<Product> findInStockProductsByCategory(Long categoryId, Pageable pageable);

    /**
     * Count products by inventory status
     */
    List<Object[]> countByInventoryStatus();

    /**
     * Find products with available quantity greater than specified amount
     */
    List<Product> findByAvailableQuantityGreaterThan(Integer minQuantity);

    /**
     * Check if product has sufficient stock
     */
    boolean hasSufficientStock(Long productId, Integer quantity);

    /**
     * Update stock quantity
     */
    boolean updateStock(Long productId, Integer quantity);

    /**
     * Reserve stock for order
     */
    boolean reserveStock(Long productId, Integer quantity);

    /**
     * Release reserved stock
     */
    boolean releaseReservedStock(Long productId, Integer quantity);

    /**
     * Deduct stock (after order completion)
     */
    boolean deductStock(Long productId, Integer quantity);

    /**
     * Update inventory status
     */
    boolean updateInventoryStatus(Long productId, InventoryStatus status);

    /**
     * Find products with stock below reorder point
     */
    List<Product> findProductsBelowReorderPoint();

    /**
     * Find new products
     */
    Page<Product> findNewProducts(Pageable pageable);

    /**
     * Find bestseller products
     */
    Page<Product> findBestsellerProducts(Pageable pageable);

    /**
     * Find products with discount
     */
    Page<Product> findDiscountedProducts(Pageable pageable);

    /**
     * Find products by multiple categories
     */
    Page<Product> findByCategories(List<Long> categoryIds, Pageable pageable);

    /**
     * Update product price
     */
    boolean updatePrice(Long productId, BigDecimal price, BigDecimal discountPrice);

    /**
     * Find products created between dates
     */
    List<Product> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Count active products by category
     */
    Long countByCategory(Long categoryId);

    /**
     * Batch update products
     */
    int batchUpdate(List<Product> products);
}

