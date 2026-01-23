package com.smart_ecomernce_api.smart_ecomernce_api.modules.category.service;

import com.smart_ecomernce_api.smart_ecomernce_api.modules.category.dto.CategoryCreateRequest;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.category.dto.CategoryResponse;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.category.dto.CategoryUpdateRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CategoryService {
    List<CategoryResponse> getAllActiveCategories();

    CategoryResponse createCategory(CategoryCreateRequest request);

    CategoryResponse getCategoryById(Long id, boolean includeChildren);

    CategoryResponse getCategoryBySlug(String slug, boolean includeChildren);

    Page<CategoryResponse> getAllCategories(Pageable pageable, Boolean isActive);

    List<CategoryResponse> getRootCategories(boolean includeChildren);

    List<CategoryResponse> getChildCategories(Long parentId, boolean includeNested);

    List<CategoryResponse> getFullHierarchy();

    CategoryResponse updateCategory(Long id, CategoryUpdateRequest request);

    CategoryResponse toggleCategoryStatus(Long id, boolean isActive);

    void deleteCategory(Long id, boolean reassignChildren);

}
