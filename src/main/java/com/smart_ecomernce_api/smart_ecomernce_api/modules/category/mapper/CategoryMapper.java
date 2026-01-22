package com.smart_ecomernce_api.smart_ecomernce_api.modules.category.mapper;

import com.smart_ecomernce_api.Smart_ecommerce_api.modules.category.dto.CategoryCreateRequest;
import com.smart_ecomernce_api.Smart_ecommerce_api.modules.category.dto.CategoryResponse;
import com.smart_ecomernce_api.Smart_ecommerce_api.modules.category.dto.CategoryUpdateRequest;
import com.smart_ecomernce_api.Smart_ecommerce_api.modules.category.entity.Category;
import org.mapstruct.*;

import java.util.stream.Collectors;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder =false))
public interface CategoryMapper {

    /**
     * Convert create request to entity
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "slug", ignore = true)
    @Mapping(target = "parent", ignore = true)
    @Mapping(target = "children", ignore = true)
    @Mapping(target = "isActive", constant = "true")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Category toEntity(CategoryCreateRequest request);

    /**
     * Convert entity to response with optional children
     */
    default CategoryResponse toResponse(Category category, boolean includeChildren) {
        if (category == null) {
            return null;
        }

        CategoryResponse response = CategoryResponse.builder().id(category.getId()).slug(category.getSlug()).name(category.getName()).description(category.getDescription()).imageUrl(category.getImageUrl()).displayOrder(category.getDisplayOrder()).level(category.getLevel()).isActive(category.getIsActive()).createdAt(category.getCreatedAt()).updatedAt(category.getUpdatedAt()).build();

        // Map parent info
        if (category.getParent() != null) {
            response.setParent(CategoryResponse.ParentCategoryInfo.builder().id(category.getParent().getId()).slug(category.getParent().getSlug()).name(category.getParent().getName()).build());
        }

        // Map children if requested
        if (includeChildren && category.getChildren() != null && !category.getChildren().isEmpty()) {
            response.setChildren(category.getChildren().stream().map(child -> toResponse(child, true)).collect(Collectors.toList()));
        }

        return response;
    }

    /**
     * Simple response mapping without children
     */
    default CategoryResponse toSimpleResponse(Category category) {
        return toResponse(category, false);
    }

    /**
     * Update entity from request (partial update)
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "slug", ignore = true)
    @Mapping(target = "parent", ignore = true)
    @Mapping(target = "children", ignore = true)
    @Mapping(target = "isActive", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromRequest(CategoryUpdateRequest request, @MappingTarget Category category);
}