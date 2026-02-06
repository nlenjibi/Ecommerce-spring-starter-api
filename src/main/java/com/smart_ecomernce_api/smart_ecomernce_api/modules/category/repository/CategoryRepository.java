package com.smart_ecomernce_api.smart_ecomernce_api.modules.category.repository;

import com.smart_ecomernce_api.smart_ecomernce_api.modules.category.entity.Category;

import java.util.List;
import java.util.Optional;

/**
 * JDBC-based Category Repository Interface
 * Supports hierarchical category structure with parent-child relationships
 */
public interface CategoryRepository {


    /**
     * Update an existing category
     */
    Category update(Category category);

    /**
     * Find category by ID
     */
    Optional<Category> findById(Long id);

    /**
     * Find active category by ID
     */
    Optional<Category> findActiveById(Long id);

    /**
     * Find category by name
     */
    Optional<Category> findByName(String name);

    /**
     * Find category by name (case-insensitive)
     */
    Optional<Category> findByNameIgnoreCase(String name);

    /**
     * Find category by slug
     */
    Optional<Category> findBySlug(String slug);

    /**
     * Find all categories
     */
    List<Category> findAll();

    /**
     * Find all categories with pagination
     */
    List<Category> findAll(int page, int size);

    /**
     * Find all active categories
     */
    List<Category> findAllActive();

    /**
     * Find all active categories with pagination
     */
    List<Category> findAllActive(int page, int size);

    /**
     * Find all root categories (categories without parent)
     */
    List<Category> findRootCategories();

    /**
     * Find all active root categories
     */
    List<Category> findActiveRootCategories();

    /**
     * Find direct children of a category
     */
    List<Category> findByParentId(Long parentId);

    /**
     * Find active direct children of a category
     */
    List<Category> findActiveByParentId(Long parentId);

    /**
     * Find all descendants of a category (recursively)
     */
    List<Category> findAllDescendants(Long categoryId);

    /**
     * Get category hierarchy path (from root to category)
     */
    List<Category> getCategoryPath(Long categoryId);

    /**
     * Count total categories
     */
    long count();

    /**
     * Count active categories
     */
    long countActive();

    /**
     * Count products in a category
     */
    long countProductsByCategoryId(Long categoryId);

    /**
     * Count children of a category
     */
    long countChildren(Long categoryId);

    /**
     * Check if category exists by ID
     */
    boolean existsById(Long id);

    /**
     * Check if category exists by name
     */
    boolean existsByName(String name);

    /**
     * Check if category exists by name (case-insensitive)
     */
    boolean existsByNameIgnoreCase(String name);

    /**
     * Check if name exists excluding specific ID (for updates)
     */
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    /**
     * Check if category exists by slug
     */
    boolean existsBySlug(String slug);

    /**
     * Check if slug exists excluding specific ID (for updates)
     */
    boolean existsBySlugAndIdNot(String slug, Long id);

    /**
     * Check if category has children
     */
    boolean hasChildren(Long categoryId);

    /**
     * Check if category has products
     */
    boolean hasProducts(Long categoryId);

    /**
     * Delete category by ID
     */
    boolean deleteById(Long id);


    /**
     * Get category level/depth in hierarchy (0 for root)
     */
    int getCategoryLevel(Long categoryId);

    /**
     * Move category to a new parent
     */
    boolean moveCategory(Long categoryId, Long newParentId);

    /**
     * Update display order
     */
    boolean updateDisplayOrder(Long categoryId, Integer displayOrder);

    /**
     * Search categories by name (partial match)
     */
    List<Category> searchByName(String searchTerm);

    /**
     * Find categories with no products
     */
    List<Category> findCategoriesWithNoProducts();

    /**
     * Save a new parent category (no parent_id)
     */
    Category saveParentCategory(Category category);

    /**
     * Save a new child category (requires parent_id)
     */
    Category saveChildCategory(Category category);

    /**
     * Find all categories by name (case-insensitive exact match)
     */
    List<Category> findAllByNameIgnoreCase(String name);
}