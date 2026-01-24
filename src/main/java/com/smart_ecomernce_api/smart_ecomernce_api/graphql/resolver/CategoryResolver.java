package com.smart_ecomernce_api.smart_ecomernce_api.graphql.resolver;


import com.smart_ecomernce_api.smart_ecomernce_api.common.response.PaginatedResponse;
import com.smart_ecomernce_api.smart_ecomernce_api.graphql.dto.CategoryResponseDto;
import com.smart_ecomernce_api.smart_ecomernce_api.graphql.input.PageInput;
import com.smart_ecomernce_api.smart_ecomernce_api.graphql.input.SortDirection;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.category.dto.CategoryCreateRequest;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.category.dto.CategoryResponse;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.category.dto.CategoryUpdateRequest;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.category.service.CategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
@RequiredArgsConstructor
@Slf4j
public class CategoryResolver {

    private final CategoryService categoryService;

    @QueryMapping
    public CategoryResponse category(@Argument Long id) {
        log.info("GraphQL Query: category(id: {})", id);
        return categoryService.getCategoryById(id, true);
    }

    @QueryMapping
    public CategoryResponse categoryBySlug(@Argument String slug) {
        log.info("GraphQL Query: categoryBySlug(slug: {})", slug);
        return categoryService.getCategoryBySlug(slug, true);
    }

    @QueryMapping
    public CategoryResponseDto categories(
            @Argument PageInput pagination,
            @Argument Boolean isActive) {
        log.info("GraphQL Query: categories");
        Pageable pageable = createPageable(pagination);
        Page<CategoryResponse> allCategories = categoryService.getAllCategories(pageable, isActive);
        return CategoryResponseDto.builder()
                .content(allCategories.getContent())
                .pageInfo(PaginatedResponse.from(allCategories))
                .build();
    }

    @QueryMapping
    public List<CategoryResponse> activeCategories() {
        log.info("GraphQL Query: activeCategories");
        return categoryService.getAllActiveCategories();
    }

    @QueryMapping
    public List<CategoryResponse> rootCategories(@Argument Boolean includeChildren) {
        log.info("GraphQL Query: rootCategories");
        return categoryService.getRootCategories(includeChildren != null ? includeChildren : false);
    }

    @QueryMapping
    public List<CategoryResponse> categoryHierarchy() {
        log.info("GraphQL Query: categoryHierarchy");
        return categoryService.getFullHierarchy();
    }

    @MutationMapping
    public CategoryResponse createCategory(@Argument CategoryCreateRequest input) {
        log.info("GraphQL Mutation: createCategory");
        return categoryService.createCategory(input);
    }

    @MutationMapping
    public CategoryResponse updateCategory(@Argument Long id, @Argument CategoryUpdateRequest input) {
        log.info("GraphQL Mutation: updateCategory(id: {})", id);
        return categoryService.updateCategory(id, input);
    }

    @MutationMapping
    public Boolean deleteCategory(@Argument Long id, @Argument Boolean reassignChildren) {
        log.info("GraphQL Mutation: deleteCategory(id: {})", id);
        categoryService.deleteCategory(id, reassignChildren != null ? reassignChildren : true);
        return true;
    }

    private Pageable createPageable(PageInput input) {
        if (input == null) {
            return PageRequest.of(0, 20, Sort.by("displayOrder"));
        }
        Sort sort = input.getDirection() == SortDirection.DESC
                ? Sort.by(input.getSortBy()).descending()
                : Sort.by(input.getSortBy()).ascending();
        return PageRequest.of(input.getPage(), input.getSize(), sort);
    }
}