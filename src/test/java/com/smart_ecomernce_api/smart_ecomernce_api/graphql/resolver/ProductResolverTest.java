package com.smart_ecomernce_api.smart_ecomernce_api.graphql.resolver;

import com.smart_ecomernce_api.smart_ecomernce_api.graphql.dto.ProductDto;
import com.smart_ecomernce_api.smart_ecomernce_api.graphql.input.PageInput;
import com.smart_ecomernce_api.smart_ecomernce_api.graphql.input.ProductFilterInput;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.product.dto.ProductCreateRequest;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.product.dto.ProductResponse;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.product.dto.ProductUpdateRequest;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.product.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductResolverTest {

    @Mock
    private ProductService productService;

    @InjectMocks
    private ProductResolver productResolver;

    private ProductResponse productResponse;
    private ProductCreateRequest createRequest;
    private ProductUpdateRequest updateRequest;
    private PageInput pageInput;
    private ProductFilterInput filterInput;

    @BeforeEach
    void setUp() {
        productResponse = ProductResponse.builder()
                .id(1L)
                .name("Test Product")
                .description("Test Description")
                .price(new BigDecimal("99.99"))
                .stockQuantity(100)
                .sku("TEST-001")
                .featured(true)
                .inStock(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        createRequest = ProductCreateRequest.builder()
                .name("New Product")
                .description("New Description")
                .price(new BigDecimal("49.99"))
                .discountedPrice(new BigDecimal("39.99"))
                .stockQuantity(50)
                .sku("NEW-001")
                .categoryId(1L)
                .build();

        updateRequest = ProductUpdateRequest.builder()
                .name("Updated Product")
                .price(new BigDecimal("59.99"))
                .stockQuantity(75)
                .build();

        pageInput = new PageInput();
        pageInput.setPage(0);
        pageInput.setSize(20);
        pageInput.setSortBy("id");
        pageInput.setDirection(com.smart_ecomernce_api.smart_ecomernce_api.graphql.input.SortDirection.ASC);

        filterInput = new ProductFilterInput();
        filterInput.setCategoryId(1L);
        filterInput.setMinPrice(new BigDecimal("10.00"));
        filterInput.setMaxPrice(new BigDecimal("100.00"));
        filterInput.setSearch("test");
        filterInput.setFeatured(false);
        filterInput.setIsNew(false);
        filterInput.setInStock(true);
    }

    @Test
    void product_ShouldReturnProduct() {
        when(productService.getProductById(1L)).thenReturn(productResponse);

        ProductResponse result = productResolver.product(1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Test Product");
        verify(productService).getProductById(1L);
    }

    @Test
    void productBySlug_ShouldReturnProduct() {
        when(productService.getProductBySlug("test-product")).thenReturn(productResponse);

        ProductResponse result = productResolver.productBySlug("test-product");

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Test Product");
        verify(productService).getProductBySlug("test-product");
    }

    @Test
    void products_WithoutFilters_ShouldReturnAllProducts() {
        List<ProductResponse> products = List.of(productResponse);
        Page<ProductResponse> productPage = new PageImpl<>(products, PageRequest.of(0, 20), 1);
        when(productService.getAllProducts(any(Pageable.class))).thenReturn(productPage);

        ProductDto result = productResolver.products(null, null);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getPageInfo()).isNotNull();
        verify(productService).getAllProducts(any(Pageable.class));
    }

    @Test
    void products_WithFilters_ShouldReturnFilteredProducts() {
        List<ProductResponse> products = List.of(productResponse);
        Page<ProductResponse> productPage = new PageImpl<>(products, PageRequest.of(0, 20), 1);
        when(productService.advancedProductSearch(
                eq(1L),
                eq(new BigDecimal("10.00")),
                eq(new BigDecimal("100.00")),
                eq("test"),
                any(Pageable.class)
        )).thenReturn(productPage);

        ProductDto result = productResolver.products(pageInput, filterInput);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getPageInfo()).isNotNull();
        verify(productService).advancedProductSearch(
                eq(1L),
                eq(new BigDecimal("10.00")),
                eq(new BigDecimal("100.00")),
                eq("test"),
                any(Pageable.class)
        );
    }

    @Test
    void featuredProducts_ShouldReturnFeaturedProducts() {
        List<ProductResponse> products = List.of(productResponse);
        Page<ProductResponse> productPage = new PageImpl<>(products, PageRequest.of(0, 20), 1);
        when(productService.getFeaturedProducts(any(Pageable.class))).thenReturn(productPage);

        ProductDto result = productResolver.featuredProducts(pageInput);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getPageInfo()).isNotNull();
        verify(productService).getFeaturedProducts(any(Pageable.class));
    }

    @Test
    void productsByCategory_ShouldReturnProductsByCategory() {
        List<ProductResponse> products = List.of(productResponse);
        Page<ProductResponse> productPage = new PageImpl<>(products, PageRequest.of(0, 20), 1);
        when(productService.getProductsByCategory(eq(1L), any(Pageable.class))).thenReturn(productPage);

        ProductDto result = productResolver.productsByCategory(1L, pageInput);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getPageInfo()).isNotNull();
        verify(productService).getProductsByCategory(eq(1L), any(Pageable.class));
    }

    @Test
    void searchProducts_ShouldReturnSearchResults() {
        List<ProductResponse> products = List.of(productResponse);
        Page<ProductResponse> productPage = new PageImpl<>(products, PageRequest.of(0, 20), 1);
        when(productService.searchProducts(eq("laptop"), any(Pageable.class))).thenReturn(productPage);

        ProductDto result = productResolver.searchProducts("laptop", pageInput);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getPageInfo()).isNotNull();
        verify(productService).searchProducts(eq("laptop"), any(Pageable.class));
    }

    @Test
    void createProduct_ShouldReturnCreatedProduct() {
        when(productService.createProduct(createRequest)).thenReturn(productResponse);

        ProductResponse result = productResolver.createProduct(createRequest);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Test Product");
        verify(productService).createProduct(createRequest);
    }

    @Test
    void updateProduct_ShouldReturnUpdatedProduct() {
        when(productService.updateProduct(1L, updateRequest)).thenReturn(productResponse);

        ProductResponse result = productResolver.updateProduct(1L, updateRequest);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        verify(productService).updateProduct(1L, updateRequest);
    }

    @Test
    void deleteProduct_ShouldReturnTrue() {
        doNothing().when(productService).deleteProduct(1L);

        Boolean result = productResolver.deleteProduct(1L);

        assertThat(result).isTrue();
        verify(productService).deleteProduct(1L);
    }
}
