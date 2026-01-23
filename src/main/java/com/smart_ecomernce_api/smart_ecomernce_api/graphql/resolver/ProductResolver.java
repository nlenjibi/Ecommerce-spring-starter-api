package com.smart_ecomernce_api.smart_ecomernce_api.graphql.resolver;

import com.smart_ecomernce_api.smart_ecomernce_api.graphql.input.PageInput;
import com.smart_ecomernce_api.smart_ecomernce_api.graphql.input.ProductFilterInput;
import com.smart_ecomernce_api.smart_ecomernce_api.graphql.input.SortDirection;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.product.dto.ProductCreateRequest;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.product.dto.ProductResponse;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.product.dto.ProductUpdateRequest;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.product.service.ProductService;
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

import java.math.BigDecimal;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ProductResolver {

    private final ProductService productService;

    @QueryMapping
    @Cacheable(value = "product", key = "#id")
    public ProductResponse product(@Argument Long id) {
        log.info("GraphQL Query: product(id: {})", id);
        return productService.getProductById(id);
    }

    @QueryMapping
    public ProductResponse productBySlug(@Argument String slug) {
        log.info("GraphQL Query: productBySlug(slug: {})", slug);
        return productService.getProductBySlug(slug);
    }

   @QueryMapping
   public Page<ProductResponse> products(
           @Argument PageInput pagination,
           @Argument ProductFilterInput filter) {
       log.info("GraphQL Query: product with pagination and filters");

       Pageable pageable = createPageable(pagination);

       if (filter != null && hasFilters(filter)) {
           return productService.advancedProductSearch(
                   filter.getCategoryId(),
                   filter.getMinPrice() != null ? filter.getMinPrice() : BigDecimal.ZERO,
                   filter.getMaxPrice() != null ? filter.getMaxPrice() : new BigDecimal("999999"),
                   filter.getSearch() != null ? filter.getSearch() : "",
                   pageable
           );
       }
       return productService.getAllProducts(pageable);
   }

    @QueryMapping
    @Cacheable(value = "product", key = "'featured_' + (#pagination==null?0:#pagination.page) + '_' + (#pagination==null?20:#pagination.size)")
    public Page<ProductResponse> featuredProducts(@Argument PageInput pagination) {
        log.info("GraphQL Query: featuredProducts");
        Pageable pageable = createPageable(pagination);
        return productService.getFeaturedProducts(pageable);
    }

    @QueryMapping
    @Cacheable(value = "product", key = "'category_' + #categoryId + '_' + (#pagination==null?0:#pagination.page) + '_' + (#pagination==null?20:#pagination.size)")
    public Page<ProductResponse> productsByCategory(
            @Argument Long categoryId,
            @Argument PageInput pagination) {
        log.info("GraphQL Query: productsByCategory(categoryId: {})", categoryId);
        Pageable pageable = createPageable(pagination);
        return productService.getProductsByCategory(categoryId, pageable);
    }

    @QueryMapping
    @Cacheable(value = "product", key = "'search_' + #search + '_' + (#pagination==null?0:#pagination.page) + '_' + (#pagination==null?20:#pagination.size)")
    public Page<ProductResponse> searchProducts(
            @Argument String search,
            @Argument PageInput pagination) {
        log.info("GraphQL Query: searchProducts(search: {})", search);
        Pageable pageable = createPageable(pagination);
        return productService.searchProducts(search, pageable);
    }

    @MutationMapping
    @CacheEvict(value = {"product", "categories"}, allEntries = true)
    public ProductResponse createProduct(@Argument ProductCreateRequest input) {
        log.info("GraphQL Mutation: createProduct");
        return productService.createProduct(input);
    }

    @MutationMapping
    @CacheEvict(value = {"product", "categories"}, allEntries = true)
    public ProductResponse updateProduct(@Argument Long id, @Argument ProductUpdateRequest input) {
        log.info("GraphQL Mutation: updateProduct(id: {})", id);
        return productService.updateProduct(id, input);
    }

    @MutationMapping
    @CacheEvict(value = {"product", "categories"}, allEntries = true)
    public Boolean deleteProduct(@Argument Long id) {
        log.info("GraphQL Mutation: deleteProduct(id: {})", id);
        productService.deleteProduct(id);
        return true;
    }

    @MutationMapping
    @CacheEvict(value = "product", allEntries = true)
    public ProductResponse reduceStock(@Argument Long id, @Argument Integer quantity) {
        log.info("GraphQL Mutation: reduceStock(id: {}, quantity: {})", id, quantity);
        return productService.reduceStock(id, quantity);
    }

    private boolean hasFilters(ProductFilterInput filter) {
        return filter.getCategoryId() != null ||
                filter.getMinPrice() != null ||
                filter.getMaxPrice() != null ||
                filter.getSearch() != null;
    }

    private Pageable createPageable(PageInput input) {
        if (input == null) {
            return PageRequest.of(0, 20, Sort.by("id"));
        }
        Sort sort = input.getDirection() == SortDirection.DESC
                ? Sort.by(input.getSortBy()).descending()
                : Sort.by(input.getSortBy()).ascending();
        return PageRequest.of(input.getPage(), input.getSize(), sort);
    }
}