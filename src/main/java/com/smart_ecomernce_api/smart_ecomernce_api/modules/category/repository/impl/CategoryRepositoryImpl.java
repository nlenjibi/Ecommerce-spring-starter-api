package com.smart_ecomernce_api.smart_ecomernce_api.modules.category.repository.impl;


import com.smart_ecomernce_api.smart_ecomernce_api.common.utils.JdbcUtils;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.category.entity.Category;

import com.smart_ecomernce_api.smart_ecomernce_api.modules.category.repository.CategoryRepository;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;

/**
 * JDBC-based Category Repository Implementation
 * Handles hierarchical category structure with parent-child relationships
 */
@Repository
public class CategoryRepositoryImpl implements CategoryRepository {

    private final JdbcUtils jdbcUtils;

    public CategoryRepositoryImpl(JdbcUtils jdbcUtils) {
        this.jdbcUtils = jdbcUtils;
    }

    /**
     * RowMapper for Category entity
     */
    private static class CategoryRowMapper implements RowMapper<Category> {
        @Override
        public Category mapRow(ResultSet rs, int rowNum) throws SQLException {
            Category category = new Category();
            category.setId(rs.getLong("id"));
            category.setName(rs.getString("name"));
            category.setSlug(rs.getString("slug"));
            category.setDescription(rs.getString("description"));
            category.setImageUrl(rs.getString("image_url"));
            category.setDisplayOrder(rs.getInt("display_order"));
            category.setIsActive(rs.getBoolean("is_active"));

            // Parent ID (will need to fetch parent separately if needed)
            Long parentId = rs.getObject("parent_id", Long.class);
            if (parentId != null) {
                Category parent = new Category();
                parent.setId(parentId);
                category.setParent(parent);
            }

            if (rs.getTimestamp("created_at") != null) {
                category.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
            }
            if (rs.getTimestamp("updated_at") != null) {
                category.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
            }

            return category;
        }
    }

    @Override
    public Category saveParentCategory(Category category) {
        String sql = """
            INSERT INTO categories (
                name, slug, description, image_url, display_order,
                is_active, created_at, updated_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """;

        JdbcUtils.QueryResult result = jdbcUtils.executePreparedQuery(sql,
                category.getName(),
                category.getSlug(),
                category.getDescription(),
                category.getImageUrl(),
                category.getDisplayOrder() != null ? category.getDisplayOrder() : 0,
                category.getIsActive() != null ? category.getIsActive() : true,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        if (result.getGeneratedKey() != null) {
            category.setId(result.getGeneratedKey());
            return findById(category.getId()).orElse(category);
        }

        // If we couldn't get a generated key, return the object as-is.
        // Any underlying DB error is already logged inside JdbcUtils.
        return category;
    }

    @Override
    public Category saveChildCategory(Category category) {
        String sql = """
            INSERT INTO categories (
                name, slug, description, image_url, display_order,
                parent_id, is_active, created_at, updated_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        JdbcUtils.QueryResult result = jdbcUtils.executePreparedQuery(sql,
                category.getName(),
                category.getSlug(),
                category.getDescription(),
                category.getImageUrl(),
                category.getDisplayOrder() != null ? category.getDisplayOrder() : 0,
                category.getParent() != null ? category.getParent().getId() : null,
                category.getIsActive() != null ? category.getIsActive() : true,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        if (result.getGeneratedKey() != null) {
            category.setId(result.getGeneratedKey());
            return findById(category.getId()).orElse(category);
        }

        return category;
    }

    @Override
    public Category update(Category category) {
        String sql = """
            UPDATE categories SET
                name = ?, slug = ?, description = ?, image_url = ?, 
                display_order = ?, parent_id = ?, is_active = ?, updated_at = ?
            WHERE id = ?
        """;

        jdbcUtils.executePreparedQuery(sql,
                category.getName(),
                category.getSlug(),
                category.getDescription(),
                category.getImageUrl(),
                category.getDisplayOrder() != null ? category.getDisplayOrder() : 0,
                category.getParent() != null ? category.getParent().getId() : null,
                category.getIsActive() != null ? category.getIsActive() : true,
                LocalDateTime.now(),
                category.getId()
        );

        return category;
    }

    @Override
    public Optional<Category> findById(Long id) {
        String sql = "SELECT * FROM categories WHERE id = ?";
        List<Category> categories = jdbcUtils.query(sql, new CategoryRowMapper(), id);
        return categories.isEmpty() ? Optional.empty() : Optional.of(categories.get(0));
    }

    @Override
    public Optional<Category> findActiveById(Long id) {
        String sql = "SELECT * FROM categories WHERE id = ? AND is_active = true";
        List<Category> categories = jdbcUtils.query(sql, new CategoryRowMapper(), id);
        return categories.isEmpty() ? Optional.empty() : Optional.of(categories.get(0));
    }

    @Override
    public Optional<Category> findByName(String name) {
        String sql = "SELECT * FROM categories WHERE name = ?";
        List<Category> categories = jdbcUtils.query(sql, new CategoryRowMapper(), name);
        return categories.isEmpty() ? Optional.empty() : Optional.of(categories.get(0));
    }

    @Override
    public Optional<Category> findByNameIgnoreCase(String name) {
        String sql = "SELECT * FROM categories WHERE LOWER(name) = LOWER(?)";
        List<Category> categories = jdbcUtils.query(sql, new CategoryRowMapper(), name);
        return categories.isEmpty() ? Optional.empty() : Optional.of(categories.get(0));
    }

    @Override
    public Optional<Category> findBySlug(String slug) {
        String sql = "SELECT * FROM categories WHERE slug = ?";
        List<Category> categories = jdbcUtils.query(sql, new CategoryRowMapper(), slug);
        return categories.isEmpty() ? Optional.empty() : Optional.of(categories.get(0));
    }

    @Override
    public List<Category> findAll() {
        String sql = "SELECT * FROM categories ORDER BY display_order ASC, name ASC";
        return jdbcUtils.query(sql, new CategoryRowMapper());
    }

    @Override
    public List<Category> findAll(int page, int size) {
        String sql = "SELECT * FROM categories ORDER BY display_order ASC, name ASC LIMIT ? OFFSET ?";
        return jdbcUtils.query(sql, new CategoryRowMapper(), size, page * size);
    }

    @Override
    public List<Category> findAllActive() {
        String sql = "SELECT * FROM categories WHERE is_active = true ORDER BY display_order ASC, name ASC";
        return jdbcUtils.query(sql, new CategoryRowMapper());
    }

    @Override
    public List<Category> findAllActive(int page, int size) {
        String sql = "SELECT * FROM categories WHERE is_active = true ORDER BY display_order ASC, name ASC LIMIT ? OFFSET ?";
        return jdbcUtils.query(sql, new CategoryRowMapper(), size, page * size);
    }

    @Override
    public List<Category> findRootCategories() {
        String sql = "SELECT * FROM categories WHERE parent_id IS NULL ORDER BY display_order ASC, name ASC";
        return jdbcUtils.query(sql, new CategoryRowMapper());
    }

    @Override
    public List<Category> findActiveRootCategories() {
        String sql = "SELECT * FROM categories WHERE parent_id IS NULL AND is_active = true ORDER BY display_order ASC, name ASC";
        return jdbcUtils.query(sql, new CategoryRowMapper());
    }

    @Override
    public List<Category> findByParentId(Long parentId) {
        String sql = "SELECT * FROM categories WHERE parent_id = ? ORDER BY display_order ASC, name ASC";
        return jdbcUtils.query(sql, new CategoryRowMapper(), parentId);
    }

    @Override
    public List<Category> findActiveByParentId(Long parentId) {
        String sql = "SELECT * FROM categories WHERE parent_id = ? AND is_active = true ORDER BY display_order ASC, name ASC";
        return jdbcUtils.query(sql, new CategoryRowMapper(), parentId);
    }

    @Override
    public List<Category> findAllDescendants(Long categoryId) {
        List<Category> descendants = new ArrayList<>();
        Queue<Long> queue = new LinkedList<>();
        queue.offer(categoryId);

        while (!queue.isEmpty()) {
            Long currentId = queue.poll();
            List<Category> children = findByParentId(currentId);

            for (Category child : children) {
                descendants.add(child);
                queue.offer(child.getId());
            }
        }

        return descendants;
    }

    @Override
    public List<Category> getCategoryPath(Long categoryId) {
        List<Category> path = new ArrayList<>();

        Optional<Category> categoryOpt = findById(categoryId);
        if (categoryOpt.isEmpty()) {
            return path;
        }

        Category current = categoryOpt.get();
        path.add(0, current);

        while (current.getParent() != null && current.getParent().getId() != null) {
            Optional<Category> parentOpt = findById(current.getParent().getId());
            if (parentOpt.isEmpty()) {
                break;
            }
            current = parentOpt.get();
            path.add(0, current);
        }

        return path;
    }

    @Override
    public long count() {
        String sql = "SELECT COUNT(*) FROM categories";
        Long count = jdbcUtils.queryForObject(sql, Long.class);
        return count != null ? count : 0L;
    }

    @Override
    public long countActive() {
        String sql = "SELECT COUNT(*) FROM categories WHERE is_active = true";
        Long count = jdbcUtils.queryForObject(sql, Long.class);
        return count != null ? count : 0L;
    }

    @Override
    public long countProductsByCategoryId(Long categoryId) {
        String sql = "SELECT COUNT(*) FROM products WHERE category_id = ?";
        Long count = jdbcUtils.queryForObject(sql, Long.class, categoryId);
        return count != null ? count : 0L;
    }

    @Override
    public long countChildren(Long categoryId) {
        String sql = "SELECT COUNT(*) FROM categories WHERE parent_id = ?";
        Long count = jdbcUtils.queryForObject(sql, Long.class, categoryId);
        return count != null ? count : 0L;
    }

    @Override
    public boolean existsById(Long id) {
        String sql = "SELECT COUNT(*) FROM categories WHERE id = ?";
        Long count = jdbcUtils.queryForObject(sql, Long.class, id);
        return count != null && count > 0;
    }

    @Override
    public boolean existsByName(String name) {
        String sql = "SELECT COUNT(*) FROM categories WHERE name = ?";
        Long count = jdbcUtils.queryForObject(sql, Long.class, name);
        return count != null && count > 0;
    }

    @Override
    public boolean existsByNameIgnoreCase(String name) {
        String sql = "SELECT COUNT(*) FROM categories WHERE LOWER(name) = LOWER(?)";
        Long count = jdbcUtils.queryForObject(sql, Long.class, name);
        return count != null && count > 0;
    }

    @Override
    public boolean existsByNameIgnoreCaseAndIdNot(String name, Long id) {
        String sql = "SELECT COUNT(*) FROM categories WHERE LOWER(name) = LOWER(?) AND id <> ?";
        Long count = jdbcUtils.queryForObject(sql, Long.class, name, id);
        return count != null && count > 0;
    }

    @Override
    public boolean existsBySlug(String slug) {
        String sql = "SELECT COUNT(*) FROM categories WHERE slug = ?";
        Long count = jdbcUtils.queryForObject(sql, Long.class, slug);
        return count != null && count > 0;
    }

    @Override
    public boolean existsBySlugAndIdNot(String slug, Long id) {
        String sql = "SELECT COUNT(*) FROM categories WHERE slug = ? AND id <> ?";
        Long count = jdbcUtils.queryForObject(sql, Long.class, slug, id);
        return count != null && count > 0;
    }

    @Override
    public boolean hasChildren(Long categoryId) {
        return countChildren(categoryId) > 0;
    }

    @Override
    public boolean hasProducts(Long categoryId) {
        return countProductsByCategoryId(categoryId) > 0;
    }

    @Override
    public boolean deleteById(Long id) {
        // Delete children first to satisfy FK constraints (hard delete)
        String deleteChildrenSql = "DELETE FROM categories WHERE parent_id = ?";
        jdbcUtils.executePreparedQuery(deleteChildrenSql, id);

        // Then delete the parent category
        String sql = "DELETE FROM categories WHERE id = ?";
        JdbcUtils.QueryResult result = jdbcUtils.executePreparedQuery(sql, id);
        return result.getAffectedRows() > 0;
    }

    @Override
    public int getCategoryLevel(Long categoryId) {
        List<Category> path = getCategoryPath(categoryId);
        return path.isEmpty() ? -1 : path.size() - 1;
    }

    @Override
    public boolean moveCategory(Long categoryId, Long newParentId) {
        // Validate: Cannot move category to itself or its descendants
        if (categoryId.equals(newParentId)) {
            return false;
        }

        if (newParentId != null) {
            List<Category> descendants = findAllDescendants(categoryId);
            for (Category descendant : descendants) {
                if (descendant.getId().equals(newParentId)) {
                    return false; // Would create circular reference
                }
            }
        }

        String sql = "UPDATE categories SET parent_id = ?, updated_at = ? WHERE id = ?";
        JdbcUtils.QueryResult result = jdbcUtils.executePreparedQuery(sql, newParentId, LocalDateTime.now(), categoryId);
        return result.getAffectedRows() > 0;
    }

    @Override
    public boolean updateDisplayOrder(Long categoryId, Integer displayOrder) {
        String sql = "UPDATE categories SET display_order = ?, updated_at = ? WHERE id = ?";
        JdbcUtils.QueryResult result = jdbcUtils.executePreparedQuery(sql, displayOrder, LocalDateTime.now(), categoryId);
        return result.getAffectedRows() > 0;
    }

    @Override
    public List<Category> searchByName(String searchTerm) {
        String sql = "SELECT * FROM categories WHERE LOWER(name) LIKE LOWER(?) ORDER BY name ASC";
        return jdbcUtils.query(sql, new CategoryRowMapper(), "%" + searchTerm + "%");
    }

    @Override
    public List<Category> findCategoriesWithNoProducts() {
        String sql = """
            SELECT c.* FROM categories c
            LEFT JOIN products p ON c.id = p.category_id
            WHERE p.id IS NULL
            ORDER BY c.name ASC
        """;
        return jdbcUtils.query(sql, new CategoryRowMapper());
    }

    @Override
    public List<Category> findAllByNameIgnoreCase(String name) {
        String sql = "SELECT * FROM categories WHERE LOWER(name) = LOWER(?) ORDER BY name ASC";
        return jdbcUtils.query(sql, new CategoryRowMapper(), name);
    }
}
