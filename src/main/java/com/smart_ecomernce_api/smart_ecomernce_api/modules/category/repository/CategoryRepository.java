package com.smart_ecomernce_api.smart_ecomernce_api.modules.category.repository;


import com.smart_ecomernce_api.smart_ecomernce_api.modules.category.entity.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findByName(String name);
    Optional<Category> findBySlug(String slug);

    boolean existsBySlug(String slug);

    boolean existsByName(String name);
    @Query("SELECT c FROM Category c WHERE c.isActive = true AND c.id = :id")
    Optional<Category> findActiveById(@Param("id") Long id);

    @Query("SELECT c FROM Category c WHERE c.isActive = true ORDER BY c.name")
    List<Category> findAllActive();

    // Use the proper path through the parent relationship (parent.id) instead of a non-existent parentId field
    @Query("SELECT c FROM Category c WHERE c.parent.id = :parentId AND c.isActive = true ORDER BY c.displayOrder ASC, c.name ASC")
    List<Category> findChildCategories(@Param("parentId") Long parentId);
    Page<Category> findByIsActiveTrue(Pageable pageable);
    List<Category> findByParentIdIsNull();


    /**
     * Find category by name (case-insensitive)
     */
    @Query("SELECT c FROM Category c WHERE LOWER(c.name) = LOWER(:name)")
    Optional<Category> findByNameIgnoreCase(@Param("name") String name);


    /**
     * Check if name exists (case-insensitive)
     */
    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM Category c WHERE LOWER(c.name) = LOWER(:name)")
    boolean existsByNameIgnoreCase(@Param("name") String name);

    /**
     * Check if name exists excluding specific ID (for updates)
     */
    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM Category c WHERE LOWER(c.name) = LOWER(:name) AND c.id <> :id")
    boolean existsByNameIgnoreCaseAndIdNot(@Param("name") String name, @Param("id") Long id);



    /**
     * Find all root categories (no parent)
     */
    @Query("SELECT c FROM Category c WHERE c.parent IS NULL ORDER BY c.displayOrder ASC, c.name ASC")
    List<Category> findRootCategories();

    /**
     * Find all active root categories
     */
    @Query("SELECT c FROM Category c WHERE c.parent IS NULL AND c.isActive = true ORDER BY c.displayOrder ASC, c.name ASC")
    List<Category> findActiveRootCategories();

    /**
     * Find direct children of a category
     */
    @Query("SELECT c FROM Category c WHERE c.parent.id = :parentId ORDER BY c.displayOrder ASC, c.name ASC")
    List<Category> findByParentId(@Param("parentId") Long parentId);

    /**
     * Find active direct children of a category
     */
    @Query("SELECT c FROM Category c WHERE c.parent.id = :parentId AND c.isActive = true ORDER BY c.displayOrder ASC, c.name ASC")
    List<Category> findActiveByParentId(@Param("parentId") Long parentId);

    /**
     * Find all categories with pagination (filtered by active status if provided)
     */
    @Query("SELECT c FROM Category c WHERE :isActive IS NULL OR c.isActive = :isActive")
    Page<Category> findAllFiltered(@Param("isActive") Boolean isActive, Pageable pageable);

    /**
     * Count product in a category
     */
    @Query("SELECT COUNT(p) FROM Product p WHERE p.category.id = :categoryId")
    Long countProductsByCategoryId(@Param("categoryId") Long categoryId);

    /**
     * Find categories by parent (including null for root categories)
     */
    List<Category> findByParent(Category parent);

    /**
     * Check if category has children
     */
    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM Category c WHERE c.parent.id = :categoryId")
    boolean hasChildren(@Param("categoryId") Long categoryId);

}