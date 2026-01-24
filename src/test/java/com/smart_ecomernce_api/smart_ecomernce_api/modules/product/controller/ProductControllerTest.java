package com.smart_ecomernce_api.smart_ecomernce_api.modules.product.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ProductControllerTest {

    @Mock
    private ProductService productService;

    @InjectMocks
    private ProductController productController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private ProductResponse productResponse;
    private ProductCreateRequest createRequest;
    private ProductUpdateRequest updateRequest;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(productController).build();
        objectMapper = new ObjectMapper();

        // Setup test data
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
    }

    @Test
    void createProduct_ShouldReturnCreatedProduct() throws Exception {
        // Given
        when(productService.createProduct(any(ProductCreateRequest.class))).thenReturn(productResponse);

        // When & Then
        mockMvc.perform(post("/v1/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Test Product"))
                .andExpect(jsonPath("$.data.price").value(99.99));

        verify(productService).createProduct(any(ProductCreateRequest.class));
    }

    @Test
    void getProductById_ShouldReturnProduct() throws Exception {
        // Given
        when(productService.getProductById(1L)).thenReturn(productResponse);

        // When & Then
        mockMvc.perform(get("/v1/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("Test Product"));

        verify(productService).getProductById(1L);
    }

    @Test
    void getAllProducts_ShouldReturnPaginatedProducts() throws Exception {
        // Given
        List<ProductResponse> products = List.of(productResponse);
        Page<ProductResponse> productPage = new PageImpl<>(products, PageRequest.of(0, 20), 1);
        when(productService.getAllProducts(any(Pageable.class))).thenReturn(productPage);

        // When & Then
        mockMvc.perform(get("/v1/products")
                .param("page", "0")
                .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].name").value("Test Product"));

        verify(productService).getAllProducts(any(Pageable.class));
    }

    @Test
    void getAllProducts_WithFilters_ShouldReturnFilteredProducts() throws Exception {
        // Given
        List<ProductResponse> products = List.of(productResponse);
        Page<ProductResponse> productPage = new PageImpl<>(products, PageRequest.of(0, 20), 1);
        when(productService.advancedProductSearch(anyLong(), any(), any(), anyString(), any(Pageable.class)))
                .thenReturn(productPage);

        // When & Then
        mockMvc.perform(get("/v1/products")
                .param("page", "0")
                .param("size", "20")
                .param("categoryId", "1")
                .param("minPrice", "10.00")
                .param("maxPrice", "100.00")
                .param("search", "test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(productService).advancedProductSearch(anyLong(), any(), any(), anyString(), any(Pageable.class));
    }

    @Test
    void getProductsByCategory_ShouldReturnProducts() throws Exception {
        // Given
        List<ProductResponse> products = List.of(productResponse);
        Page<ProductResponse> productPage = new PageImpl<>(products, PageRequest.of(0, 20), 1);
        when(productService.getProductsByCategory(eq(1L), any(Pageable.class))).thenReturn(productPage);

        // When & Then
        mockMvc.perform(get("/v1/products/category/1")
                .param("page", "0")
                .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(productService).getProductsByCategory(eq(1L), any(Pageable.class));
    }

    @Test
    void updateProduct_ShouldReturnUpdatedProduct() throws Exception {
        // Given
        when(productService.updateProduct(eq(1L), any(ProductUpdateRequest.class))).thenReturn(productResponse);

        // When & Then
        mockMvc.perform(put("/v1/products/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Test Product"));

        verify(productService).updateProduct(eq(1L), any(ProductUpdateRequest.class));
    }

    @Test
    void deleteProduct_ShouldReturnSuccess() throws Exception {
        // Given
        doNothing().when(productService).deleteProduct(1L);

        // When & Then
        mockMvc.perform(delete("/v1/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(productService).deleteProduct(1L);
    }

    @Test
    void createProduct_WithInvalidData_ShouldReturnBadRequest() throws Exception {
        // Given
        ProductCreateRequest invalidRequest = ProductCreateRequest.builder().build();

        // When & Then
        mockMvc.perform(post("/v1/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(productService, never()).createProduct(any(ProductCreateRequest.class));
    }
}
