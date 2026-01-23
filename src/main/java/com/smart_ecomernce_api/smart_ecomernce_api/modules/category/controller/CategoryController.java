package com.smart_ecomernce_api.smart_ecomernce_api.modules.category.controller;


import com.smart_ecomernce_api.smart_ecomernce_api.common.response.ApiResponse;
import com.smart_ecomernce_api.smart_ecomernce_api.common.response.PaginatedResponse;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.category.dto.CategoryCreateRequest;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.category.dto.CategoryResponse;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.category.dto.CategoryUpdateRequest;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.category.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("v1/categories")
@RequiredArgsConstructor
@Tag(name = "Category Management", description = "APIs for managing product categories")
public class CategoryController {

    private final CategoryService categoryService;

    @PostMapping
    @Operation(summary = "Create a new category", description = "Create a new product category with optional parent")
    public ResponseEntity<ApiResponse<CategoryResponse>> createCategory(
            @Valid @RequestBody CategoryCreateRequest request,
            UriComponentsBuilder uriBuilder) {
        CategoryResponse response = categoryService.createCategory(request);
        var uri = uriBuilder.path("/api/v1/categories/{id}")
                .buildAndExpand(response.getId()).toUri();
        return ResponseEntity.created(uri)
                .body(ApiResponse.success("Category created successfully", response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get category by ID", description = "Retrieve category details with parent and children info")
    public ResponseEntity<ApiResponse<CategoryResponse>> getCategoryById(
            @Parameter(description = "Category ID", required = true)
            @PathVariable Long id,
            @Parameter(description = "Include children categories")
            @RequestParam(defaultValue = "false") boolean includeChildren) {
        CategoryResponse response = categoryService.getCategoryById(id, includeChildren);
        return ResponseEntity.ok(ApiResponse.success("Category retrieved successfully", response));
    }

    @GetMapping("/slug/{slug}")
    @Operation(summary = "Get category by slug", description = "Retrieve category by its unique slug")
    public ResponseEntity<ApiResponse<CategoryResponse>> getCategoryBySlug(
            @Parameter(description = "Category slug", required = true)
            @PathVariable String slug,
            @Parameter(description = "Include children categories")
            @RequestParam(defaultValue = "false") boolean includeChildren) {
        log.info("Fetching category with slug: {}", slug);
        CategoryResponse response = categoryService.getCategoryBySlug(slug, includeChildren);
        return ResponseEntity.ok(ApiResponse.success("Category retrieved successfully", response));
    }

    @GetMapping
    @Operation(summary = "List all categories", description = "Retrieve paginated list of all categories")
    public ResponseEntity<ApiResponse<PaginatedResponse<CategoryResponse>>> getAllCategories(
            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "20")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort field", example = "displayOrder")
            @RequestParam(defaultValue = "displayOrder") String sortBy,
            @Parameter(description = "Sort direction", example = "ASC")
            @RequestParam(defaultValue = "ASC") String sortDir,
            @Parameter(description = "Filter by active status")
            @RequestParam(required = false) Boolean isActive) {

        Sort sort = sortDir.equalsIgnoreCase("DESC")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<CategoryResponse> response = categoryService.getAllCategories(pageable, isActive);
        return ResponseEntity.ok(ApiResponse.success("Categories retrieved successfully",
                PaginatedResponse.from(response)));
    }

    @GetMapping("/active")
    @Operation(summary = "List all active categories", description = "Retrieve all active categories without pagination")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getAllActiveCategories() {
        List<CategoryResponse> response = categoryService.getAllActiveCategories();
        return ResponseEntity.ok(ApiResponse.success("Active categories retrieved successfully", response));
    }

    @GetMapping("/root")
    @Operation(summary = "Get root categories", description = "Retrieve all top-level categories (without parent)")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getRootCategories(
            @Parameter(description = "Include children categories")
            @RequestParam(defaultValue = "false") boolean includeChildren) {
        List<CategoryResponse> response = categoryService.getRootCategories(includeChildren);
        return ResponseEntity.ok(ApiResponse.success("Root categories retrieved successfully", response));
    }

    @GetMapping("/{id}/children")
    @Operation(summary = "Get child categories", description = "Retrieve all direct children of a category")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getChildCategories(
            @Parameter(description = "Parent Category ID", required = true)
            @PathVariable Long id,
            @Parameter(description = "Include nested children")
            @RequestParam(defaultValue = "false") boolean includeNested) {
        List<CategoryResponse> response = categoryService.getChildCategories(id, includeNested);
        return ResponseEntity.ok(ApiResponse.success("Child categories retrieved successfully", response));
    }

    @GetMapping("/hierarchy")
    @Operation(summary = "Get full category hierarchy", description = "Retrieve complete category tree structure")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getCategoryHierarchy() {
        List<CategoryResponse> response = categoryService.getFullHierarchy();
        return ResponseEntity.ok(ApiResponse.success("Category hierarchy retrieved successfully", response));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update category", description = "Update category details including parent relationship")
    public ResponseEntity<ApiResponse<CategoryResponse>> updateCategory(
            @Parameter(description = "Category ID", required = true)
            @PathVariable Long id,
            @Valid @RequestBody CategoryUpdateRequest request) {
        CategoryResponse response = categoryService.updateCategory(id, request);
        return ResponseEntity.ok(ApiResponse.success("Category updated successfully", response));
    }

    @PatchMapping("/{id}/activate")
    @Operation(summary = "Activate category", description = "Set category as active")
    public ResponseEntity<ApiResponse<CategoryResponse>> activateCategory(
            @PathVariable Long id) {
        CategoryResponse response = categoryService.toggleCategoryStatus(id, true);
        return ResponseEntity.ok(ApiResponse.success("Category activated successfully", response));
    }

    @PatchMapping("/{id}/deactivate")
    @Operation(summary = "Deactivate category", description = "Set category as inactive")
    public ResponseEntity<ApiResponse<CategoryResponse>> deactivateCategory(
            @PathVariable Long id) {
        CategoryResponse response = categoryService.toggleCategoryStatus(id, false);
        return ResponseEntity.ok(ApiResponse.success("Category deactivated successfully", response));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete category", description = "Delete category and optionally reassign its children")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(
            @Parameter(description = "Category ID", required = true)
            @PathVariable Long id,
            @Parameter(description = "Reassign children to parent before deletion")
            @RequestParam(defaultValue = "true") boolean reassignChildren) {
        categoryService.deleteCategory(id, reassignChildren);
        return ResponseEntity.ok(ApiResponse.success("Category deleted successfully", null));
    }
}