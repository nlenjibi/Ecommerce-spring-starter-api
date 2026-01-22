package com.smart_ecomernce_api.smart_ecomernce_api.modules.product.controller;

import com.smart_ecomernce_api.Smart_ecommerce_api.common.response.ApiResponse;
import com.smart_ecomernce_api.Smart_ecommerce_api.common.response.PaginatedResponse;
import com.smart_ecomernce_api.Smart_ecommerce_api.modules.product.dto.ProductCreateRequest;
import com.smart_ecomernce_api.Smart_ecommerce_api.modules.product.dto.ProductResponse;
import com.smart_ecomernce_api.Smart_ecommerce_api.modules.product.dto.ProductUpdateRequest;
import com.smart_ecomernce_api.Smart_ecommerce_api.modules.product.service.ProductService;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@Slf4j
@RestController
@RequestMapping("v1/products")
@RequiredArgsConstructor
@Tag(name = "Product Management", description = "APIs for managing product with advanced filtering")
public class ProductController {

    private final ProductService productService;

    @PostMapping
    @Operation(summary = "Create a new product", description = "Create a new product with details")
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
            @Valid @RequestBody ProductCreateRequest request) {
        ProductResponse response = productService.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Product created successfully", response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get product by ID", description = "Retrieve product details by product ID")
    public ResponseEntity<ApiResponse<ProductResponse>> getProductById(
            @Parameter(description = "Product ID", required = true)
            @PathVariable Long id) {
        ProductResponse response = productService.getProductById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    @Operation(summary = "List all product with advanced filtering",
            description = "Retrieve paginated list of product with optional filtering by category, price range, and search")
    public ResponseEntity<ApiResponse<PaginatedResponse<ProductResponse>>> getAllProducts(
            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "20")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort field", example = "price")
            @RequestParam(defaultValue = "id") String sortBy,
            @Parameter(description = "Sort direction", example = "ASC")
            @RequestParam(defaultValue = "ASC") Sort.Direction direction,
            @Parameter(description = "Filter by category ID (optional)")
            @RequestParam(required = false) Long categoryId,
            @Parameter(description = "Minimum price (optional)")
            @RequestParam(required = false) BigDecimal minPrice,
            @Parameter(description = "Maximum price (optional)")
            @RequestParam(required = false) BigDecimal maxPrice,
            @Parameter(description = "Search product by name (optional)")
            @RequestParam(required = false) String search) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        Page<ProductResponse> response;

        // Advanced search with all filters
        if ((categoryId != null || minPrice != null || maxPrice != null) || search != null) {
            Long filterCategoryId = categoryId;
            BigDecimal filterMinPrice = minPrice != null ? minPrice : BigDecimal.ZERO;
            BigDecimal filterMaxPrice = maxPrice != null ? maxPrice : new BigDecimal("999999");
            String filterSearch = search != null ? search : "";

            response = productService.advancedProductSearch(filterCategoryId, filterMinPrice, filterMaxPrice, filterSearch, pageable);
        } else {
            response = productService.getAllProducts(pageable);
        }

        PaginatedResponse<ProductResponse> paginatedResponse = PaginatedResponse.from(response);
        return ResponseEntity.ok(ApiResponse.success(paginatedResponse));
    }

    @GetMapping("/category/{categoryId}")
    @Operation(summary = "Get product by category", description = "Retrieve product filtered by category")
    public ResponseEntity<ApiResponse<PaginatedResponse<ProductResponse>>> getProductsByCategory(
            @Parameter(description = "Category ID", required = true)
            @PathVariable Long categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "ASC") Sort.Direction direction) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        Page<ProductResponse> response = productService.getProductsByCategory(categoryId, pageable);
        PaginatedResponse<ProductResponse> paginatedResponse = PaginatedResponse.from(response);
        return ResponseEntity.ok(ApiResponse.success(paginatedResponse));
    }

    @GetMapping("/price-range")
    @Operation(summary = "Get product by price range", description = "Retrieve product filtered by price range")
    public ResponseEntity<ApiResponse<PaginatedResponse<ProductResponse>>> getProductsByPriceRange(
            @Parameter(description = "Minimum price", required = true)
            @RequestParam BigDecimal minPrice,
            @Parameter(description = "Maximum price", required = true)
            @RequestParam BigDecimal maxPrice,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "price") String sortBy,
            @RequestParam(defaultValue = "ASC") Sort.Direction direction) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        Page<ProductResponse> response = productService.getProductsByPriceRange(minPrice, maxPrice, pageable);
        PaginatedResponse<ProductResponse> paginatedResponse = PaginatedResponse.from(response);
        return ResponseEntity.ok(ApiResponse.success(paginatedResponse));
    }

    @GetMapping("/search")
    @Operation(summary = "Search product by name", description = "Retrieve product matching search query")
    public ResponseEntity<ApiResponse<PaginatedResponse<ProductResponse>>> searchProducts(
            @Parameter(description = "Search term", required = true)
            @RequestParam(value = "q", defaultValue = "") String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "ASC") Sort.Direction direction) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        Page<ProductResponse> response = productService.searchProducts(search, pageable);
        PaginatedResponse<ProductResponse> paginatedResponse = PaginatedResponse.from(response);
        return ResponseEntity.ok(ApiResponse.success(paginatedResponse));
    }

    @GetMapping("/featured")
    @Operation(summary = "Get featured product", description = "Retrieve featured product")
    public ResponseEntity<ApiResponse<PaginatedResponse<ProductResponse>>> getFeaturedProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<ProductResponse> products = productService.getFeaturedProducts(pageable);
        PaginatedResponse<ProductResponse> paginatedResponse = PaginatedResponse.from(products);

        return ResponseEntity.ok(ApiResponse.success(paginatedResponse));
    }

    @GetMapping("slug/{slug}")
    @Operation(summary = "Get product by slug", description = "Retrieve product details by product slug")
    public ResponseEntity<ApiResponse<ProductResponse>> getProductBySlug(
            @Parameter(description = "Product slug", required = true)
            @PathVariable String slug) {
        log.info("Fetching product with slug: {}", slug);
        ProductResponse product = productService.getProductBySlug(slug);
        return ResponseEntity.ok(ApiResponse.success(product));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update product", description = "Update product details by product ID")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @Parameter(description = "Product ID", required = true)
            @PathVariable Long id,
            @Valid @RequestBody ProductUpdateRequest request) {
        ProductResponse response = productService.updateProduct(id, request);
        return ResponseEntity.ok(ApiResponse.success("Product updated successfully", response));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete product", description = "Delete product by product ID")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(
            @Parameter(description = "Product ID", required = true)
            @PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.ok(ApiResponse.success("Product deleted successfully", null));
    }

    @PostMapping("/{id}/reduce-stock")
    @Operation(summary = "Reduce product stock", description = "Reduce stock quantity for a product")
    public ResponseEntity<ApiResponse<ProductResponse>> reduceStock(
            @Parameter(description = "Product ID", required = true)
            @PathVariable Long id,
            @Parameter(description = "Quantity to reduce", required = true)
            @RequestParam Integer quantity) {
        ProductResponse response = productService.reduceStock(id, quantity);
        return ResponseEntity.ok(ApiResponse.success("Stock reduced successfully", response));
    }
}