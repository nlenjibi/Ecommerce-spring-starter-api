package com.smart_ecomernce_api.smart_ecomernce_api.modules.product.repository;

import com.smart_ecomernce_api.Smart_ecommerce_api.modules.product.entity.InventoryStatus;
import com.smart_ecomernce_api.Smart_ecommerce_api.modules.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {
    Optional<Product> findBySlug(String slug);

    Optional<Product> findBySku(String sku);

    boolean existsBySlug(String slug);

    Page<Product> findByFeaturedTrueAndIsActiveTrue(Pageable pageable);

    List<Product> findByFeaturedTrue();

    /**
     * Find product by inventory status
     */
    List<Product> findByInventoryStatus(InventoryStatus status);

    /**
     * Find product with low stock
     */
    @Query("SELECT p FROM Product p WHERE p.inventoryStatus = 'LOW_STOCK' " +
            "AND p.trackInventory = true AND p.isActive = true")
    List<Product> findLowStockProducts();

    /**
     * Find out of stock product
     */
    @Query("SELECT p FROM Product p WHERE p.inventoryStatus = 'OUT_OF_STOCK' " +
            "AND p.trackInventory = true AND p.isActive = true")
    List<Product> findOutOfStockProducts();

    /**
     * Find product that need reorder
     */
    @Query("SELECT p FROM Product p WHERE p.stockQuantity <= p.reorderPoint " +
            "AND p.trackInventory = true AND p.isActive = true")
    List<Product> findProductsNeedingReorder();

    /**
     * Find in-stock product by category
     */
    @Query("SELECT p FROM Product p WHERE p.category.id = :categoryId " +
            "AND p.inventoryStatus IN ('IN_STOCK', 'LOW_STOCK') " +
            "AND p.isActive = true")
    Page<Product> findInStockProductsByCategory(
            @Param("categoryId") Long categoryId,
            Pageable pageable
    );

    /**
     * Count product by inventory status
     */
    @Query("SELECT p.inventoryStatus, COUNT(p) FROM Product p " +
            "WHERE p.trackInventory = true " +
            "GROUP BY p.inventoryStatus")
    List<Object[]> countByInventoryStatus();

    /**
     * Find product with available quantity greater than specified amount
     */
    @Query("SELECT p FROM Product p " +
            "WHERE (p.stockQuantity - p.reservedQuantity) >= :minQuantity " +
            "AND p.isActive = true")
    List<Product> findByAvailableQuantityGreaterThan(
            @Param("minQuantity") Integer minQuantity
    );

    /**
     * Check if product has sufficient stock
     */
    @Query("SELECT CASE WHEN (p.stockQuantity - p.reservedQuantity) >= :quantity " +
            "THEN true ELSE false END " +
            "FROM Product p WHERE p.id = :productId")
    boolean hasSufficientStock(
            @Param("productId") Long productId,
            @Param("quantity") Integer quantity
    );

    @Query("SELECT p FROM Product p WHERE p.isActive = true AND p.id = :id")
    Optional<Product> findActiveById(@Param("id") Long id);

    @Query("SELECT p FROM Product p WHERE p.category.id = :categoryId AND p.isActive = true")
    Page<Product> findByCategory(@Param("categoryId") Long categoryId, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE " +
            "p.isActive = true AND " +
            "p.discountPrice >= :minPrice AND " +
            "p.discountPrice <= :maxPrice")
    Page<Product> findByPriceRange(@Param("minPrice") BigDecimal minPrice,
                                   @Param("maxPrice") BigDecimal maxPrice,
                                   Pageable pageable);

    @Query("SELECT p FROM Product p WHERE " +
            "p.isActive = true AND " +
            "LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<Product> searchByName(@Param("search") String search, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE " +
            "p.isActive = true AND " +
            "p.category.id = :categoryId AND " +
            "p.discountPrice >= :minPrice AND " +
            "p.discountPrice <= :maxPrice AND " +
            "LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<Product> findByAdvancedFilters(@Param("categoryId") Long categoryId,
                                        @Param("minPrice") BigDecimal minPrice,
                                        @Param("maxPrice") BigDecimal maxPrice,
                                        @Param("search") String search,
                                        Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.isActive = true AND " +
            "(LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Product> searchProducts(@Param("keyword") String keyword, Pageable pageable);
}