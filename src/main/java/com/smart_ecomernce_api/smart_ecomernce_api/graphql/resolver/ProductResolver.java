package com.smart_ecomernce_api.smart_ecomernce_api.graphql.resolver;

import com.smart_ecomernce_api.smart_ecomernce_api.common.response.PaginatedResponse;
import com.smart_ecomernce_api.smart_ecomernce_api.graphql.dto.ProductDto;
import com.smart_ecomernce_api.smart_ecomernce_api.graphql.input.PageInput;
import com.smart_ecomernce_api.smart_ecomernce_api.graphql.input.ProductFilterInput;
import com.smart_ecomernce_api.smart_ecomernce_api.graphql.input.SortDirection;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.product.dto.ProductCreateRequest;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.product.dto.ProductResponse;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.product.dto.ProductUpdateRequest;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.product.mapper.ProductMapper;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    public ProductDto products(
            @Argument PageInput pagination,
            @Argument ProductFilterInput filter) {
        log.info("GraphQL Query: products with pagination and filters");

        Pageable pageable = createPageable(pagination);
           Page<ProductResponse> productPage;

        if (filter != null && hasFilters(filter)) {
             productPage = productService.advancedProductSearch(
                    filter.getCategoryId(),
                    filter.getMinPrice() != null ? filter.getMinPrice() : BigDecimal.ZERO,
                    filter.getMaxPrice() != null ? filter.getMaxPrice() : new BigDecimal("999999"),
                    filter.getSearch() != null ? filter.getSearch() : "",
                    pageable
            );
        }else {
        productPage = productService.getAllProducts(pageable);
        }
           return  ProductDto.builder()
                   .content(productPage.getContent())
                   .pageInfo(PaginatedResponse.from(productPage))
                   .build();
    }

    @QueryMapping
    public ProductDto featuredProducts(@Argument PageInput pagination) {
        log.info("GraphQL Query: featuredProducts");
        Pageable pageable = createPageable(pagination);
        Page<ProductResponse> productPage;

         productPage = productService.getFeaturedProducts(pageable);

       return  ProductDto.builder()
               .content(productPage.getContent())
            .pageInfo(PaginatedResponse.from(productPage))
            .build();
    }

    @QueryMapping
    public ProductDto productsByCategory(
            @Argument Long categoryId,
            @Argument PageInput pagination) {
        log.info("GraphQL Query: productsByCategory(categoryId: {})", categoryId);
        Pageable pageable = createPageable(pagination);
        Page<ProductResponse> productPage;
        productPage = productService.getProductsByCategory(categoryId, pageable);


        return  ProductDto.builder()
                .content(productPage.getContent())
                .pageInfo(PaginatedResponse.from(productPage))
                .build();
    }

    @QueryMapping
    public ProductDto searchProducts(
            @Argument String search,
            @Argument PageInput pagination) {
        log.info("GraphQL Query: searchProducts(search: {})", search);
        Pageable pageable = createPageable(pagination);
        Page<ProductResponse> productPage;
         productPage = productService.searchProducts(search, pageable);

        return  ProductDto.builder()
                .content(productPage.getContent())
                .pageInfo(PaginatedResponse.from(productPage))
                .build();
    }

    @MutationMapping
    public ProductResponse createProduct(@Argument ProductCreateRequest input) {
        log.info("GraphQL Mutation: createProduct");
        return productService.createProduct(input);
    }

    @MutationMapping
    public ProductResponse updateProduct(@Argument Long id, @Argument ProductUpdateRequest input) {
        log.info("GraphQL Mutation: updateProduct(id: {})", id);
        return productService.updateProduct(id, input);
    }

    @MutationMapping
    public Boolean deleteProduct(@Argument Long id) {
        log.info("GraphQL Mutation: deleteProduct(id: {})", id);
        productService.deleteProduct(id);
        return true;
    }

    @MutationMapping
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