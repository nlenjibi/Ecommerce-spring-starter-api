package com.smart_ecomernce_api.smart_ecomernce_api.modules.product.service;


import com.smart_ecomernce_api.Smart_ecommerce_api.modules.product.dto.ProductCreateRequest;
import com.smart_ecomernce_api.Smart_ecommerce_api.modules.product.dto.ProductResponse;
import com.smart_ecomernce_api.Smart_ecommerce_api.modules.product.dto.ProductUpdateRequest;
import com.smart_ecomernce_api.Smart_ecommerce_api.modules.product.entity.InventoryStatus;
import com.smart_ecomernce_api.Smart_ecommerce_api.modules.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

public interface ProductService {
    ProductResponse createProduct(ProductCreateRequest request);
    ProductResponse updateProduct(Long id, ProductUpdateRequest request);
    ProductResponse getProductById(Long id);
    ProductResponse getProductBySlug(String slug);

    Page<ProductResponse> getAllProducts(Pageable pageable);
    Page<ProductResponse> getProductsByCategory(Long categoryId, Pageable pageable);

    Page<ProductResponse> getProductsByPriceRange(BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);
    Page<ProductResponse> advancedProductSearch(Long categoryId, BigDecimal minPrice, BigDecimal maxPrice, String search, Pageable pageable);
    Page<ProductResponse> searchProducts(String search, Pageable pageable);
    Page<ProductResponse> getFeaturedProducts(Pageable pageable);
    void deleteProduct(Long id);
    ProductResponse reduceStock(Long productId, Integer quantity);

    // Find product by inventory status
    List<Product> findByInventoryStatus(InventoryStatus status);
    public List<Product> getLowStockProducts();
    public List<Product> getProductsNeedingReorder();

    void restoreStock(Long productId, Integer quantity);
}