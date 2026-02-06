package com.smart_ecomernce_api.smart_ecomernce_api.modules.category.service.impl;

import com.smart_ecomernce_api.smart_ecomernce_api.common.utils.SlugGenerator;
import com.smart_ecomernce_api.smart_ecomernce_api.exception.DuplicateResourceException;
import com.smart_ecomernce_api.smart_ecomernce_api.exception.InvalidDataException;
import com.smart_ecomernce_api.smart_ecomernce_api.exception.ResourceNotFoundException;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.category.dto.CategoryCreateRequest;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.category.dto.CategoryResponse;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.category.dto.CategoryUpdateRequest;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.category.entity.Category;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.category.mapper.CategoryMapper;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.category.repository.CategoryRepository;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.category.service.CategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final SlugGenerator slugGenerator;
    private final CategoryMapper categoryMapper;

    @Override
    @Transactional
    public CategoryResponse createCategory(CategoryCreateRequest request) {
        if (hasParentId(request.getParentId())) {
            return createChildCategory(request);
        }

        return createParentCategory(request);
    }

    @Override
    @Transactional
    public CategoryResponse createParentCategory(CategoryCreateRequest request) {
        if (hasParentId(request.getParentId())) {
            throw new InvalidDataException("Parent category ID is not allowed for parent category creation");
        }

        Category category = prepareCategoryForCreate(request, null);
        Category savedCategory = categoryRepository.saveParentCategory(category);
        log.info("Parent category created: id={}, slug={}", savedCategory.getId(), savedCategory.getSlug());
        return categoryMapper.toResponse(savedCategory, false);
    }

    @Override
    @Transactional
    public CategoryResponse createChildCategory(CategoryCreateRequest request) {
        if (!hasParentId(request.getParentId())) {
            throw new InvalidDataException("Parent category ID is required for child category creation");
        }

        Category parent = categoryRepository.findById(request.getParentId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Parent category not found with id: " + request.getParentId()));

        if (!parent.getIsActive()) {
            throw new InvalidDataException("Cannot assign inactive parent category");
        }

        Category category = prepareCategoryForCreate(request, parent);
        Category savedCategory = categoryRepository.saveChildCategory(category);
        log.info("Child category created: id={}, slug={}", savedCategory.getId(), savedCategory.getSlug());
        return categoryMapper.toResponse(savedCategory, false);
    }

    private Category prepareCategoryForCreate(CategoryCreateRequest request, Category parent) {
        log.info("Preparing category for create: name={}, parentId={}", request.getName(), request.getParentId());

        // Validate name uniqueness
        if (categoryRepository.existsByNameIgnoreCase(request.getName())) {
            throw new DuplicateResourceException("Category with name '" + request.getName() + "' already exists");
        }

        // Generate unique slug
        String baseSlug = slugGenerator.generateSlug(request.getName());
        String uniqueSlug = generateUniqueSlug(baseSlug);

        Category category = categoryMapper.toEntity(request);
        category.setSlug(uniqueSlug);
        category.setDisplayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0);
        category.setParent(parent);

        return category;
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(Long id, boolean includeChildren) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));

        CategoryResponse response = categoryMapper.toResponse(category, includeChildren);
        response.setProductCount(categoryRepository.countProductsByCategoryId(id));
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getCategoryBySlug(String slug, boolean includeChildren) {
        log.debug("Fetching category by slug: {}", slug);
        Category category = categoryRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with slug: " + slug));

        CategoryResponse response = categoryMapper.toResponse(category, includeChildren);
        response.setProductCount(categoryRepository.countProductsByCategoryId(category.getId()));
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CategoryResponse> getAllCategories(Pageable pageable, Boolean isActive) {
        List<Category> categories;
        long total;

        if (isActive == null) {
            categories = categoryRepository.findAll(pageable.getPageNumber(), pageable.getPageSize());
            total = categoryRepository.count();
        } else if (isActive) {
            categories = categoryRepository.findAllActive(pageable.getPageNumber(), pageable.getPageSize());
            total = categoryRepository.countActive();
        } else {
            // For inactive categories, since there's no specific method, use findAll and filter
            categories = categoryRepository.findAll(pageable.getPageNumber(), pageable.getPageSize())
                    .stream()
                    .filter(category -> !category.getIsActive())
                    .collect(Collectors.toList());
            total = categoryRepository.count() - categoryRepository.countActive();
        }

        List<CategoryResponse> responses = categories.stream()
                .map(category -> {
                    CategoryResponse response = categoryMapper.toResponse(category, false);
                    response.setProductCount(categoryRepository.countProductsByCategoryId(category.getId()));
                    return response;
                })
                .collect(Collectors.toList());

        return new PageImpl<>(responses, pageable, total);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllActiveCategories() {
        return categoryRepository.findAllActive().stream()
                .map(category -> categoryMapper.toResponse(category, false))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getRootCategories(boolean includeChildren) {
        List<Category> rootCategories = categoryRepository.findActiveRootCategories();
        return rootCategories.stream()
                .map(category -> categoryMapper.toResponse(category, includeChildren))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getChildCategories(Long parentId, boolean includeNested) {
        // Verify parent exists
        if (!categoryRepository.existsById(parentId)) {
            throw new ResourceNotFoundException("Parent category not found with id: " + parentId);
        }

        List<Category> children = categoryRepository.findActiveByParentId(parentId);
        return children.stream()
                .map(category -> categoryMapper.toResponse(category, includeNested))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getFullHierarchy() {
        List<Category> rootCategories = categoryRepository.findActiveRootCategories();
        return rootCategories.stream()
                .map(category -> categoryMapper.toResponse(category, true))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CategoryResponse updateCategory(Long id, CategoryUpdateRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));

        // Check name uniqueness if name is being changed
        if (request.getName() != null &&
                !request.getName().equalsIgnoreCase(category.getName()) &&
                categoryRepository.existsByNameIgnoreCaseAndIdNot(request.getName(), id)) {
            throw new DuplicateResourceException("Category with name '" + request.getName() + "' already exists");
        }

        // Update basic fields
        if (request.getName() != null) {
            category.setName(request.getName());
            // Regenerate slug if name changed
            String newSlug = generateUniqueSlug(slugGenerator.generateSlug(request.getName()));
            if (!newSlug.equals(category.getSlug())) {
                category.setSlug(newSlug);
            }
        }
        if (request.getDescription() != null) {
            category.setDescription(request.getDescription());
        }
        if (request.getImageUrl() != null) {
            category.setImageUrl(request.getImageUrl());
        }
        if (request.getDisplayOrder() != null) {
            category.setDisplayOrder(request.getDisplayOrder());
        }

        // Update parent relationship
        if (request.getParentId() != null) {
            if (!hasParentId(request.getParentId())) {
                category.setParent(null);
            } else {
                // Prevent self-reference
                if (request.getParentId().equals(id)) {
                    throw new InvalidDataException("Category cannot be its own parent");
                }

                // Prevent circular reference
                if (wouldCreateCircularReference(id, request.getParentId())) {
                    throw new InvalidDataException("Cannot set parent: would create circular reference");
                }

                Category newParent = categoryRepository.findById(request.getParentId())
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Parent category not found with id: " + request.getParentId()));

                category.setParent(newParent);
            }
        } else if (request.getParentId() == null && category.getParent() != null) {
            // Explicitly set to null to make it a root category
            category.setParent(null);
        }

        Category updatedCategory = categoryRepository.update(category);
        log.info("Category updated: id={}, name={}", id, updatedCategory.getName());

        return categoryMapper.toResponse(updatedCategory, false);
    }

    @Override
    @Transactional
    public CategoryResponse toggleCategoryStatus(Long id, boolean isActive) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));

        category.setIsActive(isActive);

        // Optionally cascade to children
        if (!isActive) {
            List<Category> children = categoryRepository.findByParentId(id);
            children.forEach(child -> child.setIsActive(false));
        }

        Category savedCategory = categoryRepository.update(category);
        log.info("Category status updated: id={}, isActive={}", id, isActive);

        return categoryMapper.toResponse(savedCategory, false);
    }

    @Override
    @Transactional
    public void deleteCategory(Long id, boolean reassignChildren) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));

        // Hard delete: do NOT reassign children anymore. Delete subtree.
        // Block deletion if this category or ANY descendant has products.
        long productCount = categoryRepository.countProductsByCategoryId(id);

        List<Category> descendants = categoryRepository.findAllDescendants(id);
        for (Category descendant : descendants) {
            if (descendant.getId() == null) {
                continue;
            }
            productCount += categoryRepository.countProductsByCategoryId(descendant.getId());
        }

        if (productCount > 0) {
            throw new InvalidDataException(
                    "Cannot delete category because it (or its subcategories) contains " + productCount + " product(s). " +
                            "Please reassign or delete products first.");
        }

        categoryRepository.deleteById(category.getId());
        log.info("Category deleted (hard delete): id={}, name={}", id, category.getName());
    }

    /**
     * Check if setting newParentId as parent would create circular reference
     */
    private boolean wouldCreateCircularReference(Long categoryId, Long newParentId) {
        Category current = categoryRepository.findById(newParentId).orElse(null);

        while (current != null) {
            if (current.getId().equals(categoryId)) {
                return true;
            }
            current = current.getParent();
        }

        return false;
    }

    /**
     * Generate unique slug by appending counter if needed
     */
    private String generateUniqueSlug(String baseSlug) {
        String uniqueSlug = baseSlug;
        int counter = 0;

        while (categoryRepository.existsBySlug(uniqueSlug)) {
            counter++;
            uniqueSlug = baseSlug + "-" + counter;
        }

        return uniqueSlug;
    }

    private boolean hasParentId(Long parentId) {
        return parentId != null && parentId > 0;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> searchCategoriesByName(String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return List.of();
        }

        return categoryRepository.searchByName(searchTerm.trim()).stream()
                .map(category -> {
                    CategoryResponse response = categoryMapper.toResponse(category, false);
                    response.setProductCount(categoryRepository.countProductsByCategoryId(category.getId()));
                    return response;
                })
                .collect(Collectors.toList());
    }
}
