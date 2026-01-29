package com.smart_ecomernce_api.smart_ecomernce_api.modules.product.service.impl;


import com.smart_ecomernce_api.smart_ecomernce_api.common.utils.SlugGenerator;
import com.smart_ecomernce_api.smart_ecomernce_api.exception.InsufficientStockException;
import com.smart_ecomernce_api.smart_ecomernce_api.exception.InvalidDataException;
import com.smart_ecomernce_api.smart_ecomernce_api.exception.ResourceNotFoundException;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.category.entity.Category;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.category.repository.CategoryRepository;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.product.dto.ProductCreateRequest;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.product.dto.ProductResponse;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.product.dto.ProductUpdateRequest;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.product.entity.InventoryStatus;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.product.entity.Product;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.product.mapper.ProductMapper;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.product.repository.ProductRepository;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductServiceImpl implements ProductService {
    private final ProductMapper productMapper;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final SlugGenerator slugGenerator;

    @Override
    @Transactional
    @CacheEvict(value = {"product", "categories"}, allEntries = true)
    public ProductResponse createProduct(ProductCreateRequest request) {
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> ResourceNotFoundException.forResource("Category", request.getCategoryId()));

        if (productRepository.findBySku(request.getSku()).isPresent()) {
            throw new InvalidDataException("Product with SKU " + request.getSku() + " already exists");
        }
        log.info("Creating new product: {}", request.getName());

       // Generate unique slug
        String baseSlug = slugGenerator.generateSlug(request.getName());
        String uniqueSlug = baseSlug;
        int counter = 1;
        while (productRepository.existsBySlug(uniqueSlug)) {
            uniqueSlug = baseSlug + "-" + counter++;
        }

        var product = productMapper.toEntity(request);
        product.setCategory(category);
        product.setSlug(uniqueSlug);
        Product savedProduct = productRepository.save(product);
        log.info("Product created with id: {}", savedProduct.getId());
        return productMapper.toDto(savedProduct);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"product", "categories"}, allEntries = true)
    public ProductResponse updateProduct(Long id, ProductUpdateRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.forResource("Product", id));

        Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> ResourceNotFoundException.forResource("Category", request.getCategoryId()));
            product.setCategory(category);

        productMapper.update(request, product);
        product.setCategory(category);

        Product updatedProduct = productRepository.save(product);
        log.info("Product updated with id: {}", id);
        return productMapper.toDto(updatedProduct);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long id) {
        Product product = productRepository.findActiveById(id)
                .orElseThrow(() -> ResourceNotFoundException.forResource("Product", id));
        return productMapper.toDto(product);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> getAllProducts(Pageable pageable) {
        return productRepository.findAll(pageable).map(productMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> getProductsByCategory(Long categoryId, Pageable pageable) {
        categoryRepository.findById(categoryId)
                .orElseThrow(() -> ResourceNotFoundException.forResource("Category", categoryId));
        return productRepository.findByCategory(categoryId, pageable).map(productMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> getProductsByPriceRange(BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable) {
        return productRepository.findByPriceRange(minPrice, maxPrice, pageable).map(productMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> searchProducts(String keyword, Pageable pageable) {
        return productRepository.searchProducts(keyword, pageable).map(productMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> advancedProductSearch(Long categoryId, BigDecimal minPrice, BigDecimal maxPrice, String search, Pageable pageable) {
        return productRepository.findByAdvancedFilters(categoryId, minPrice, maxPrice, search, pageable)
                .map(productMapper::toDto);
    }

    @Override
    @Transactional
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.forResource("Product", id));
        productRepository.delete(product);
        log.info("Product deleted with id: {}", id);
    }

    @Override
    @Transactional
    @CacheEvict(value = "product", allEntries = true)
    public ProductResponse reduceStock(Long productId, Integer quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> ResourceNotFoundException.forResource("Product", productId));

        if (product.getStockQuantity() < quantity) {
            throw new InvalidDataException("Insufficient stock. Available: " + product.getStockQuantity());
        }

        product.setStockQuantity(product.getStockQuantity() - quantity);
        Product updatedProduct = productRepository.save(product);
        log.info("Product stock reduced for id: {}", productId);
        return productMapper.toDto(updatedProduct);
    }

      /**
     * Get product by slug - CACHED
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "product-by-slug", key = "#slug")
    @Override
    public ProductResponse getProductBySlug(String slug) {
        log.info("Fetching product by slug: {}", slug);
        Product product = productRepository.findBySlug(slug)
                .orElseThrow(() -> ResourceNotFoundException.forResource("Product Slug", slug));
        return productMapper.toDto(product);
    }

    /**
     * Get featured product - CACHED
     */
    @Cacheable(value = "featured-product")
    @Transactional(readOnly = true)
    public Page<ProductResponse> getFeaturedProducts(Pageable pageable) {
        log.info("Fetching featured product");
        Page<Product> products = productRepository.findByFeaturedTrueAndIsActiveTrue(pageable);
        return products.map(productMapper::toDto);
    }

    /**
     * Reserve stock for order
     */

    public void reserveStock(Long productId, int quantity) {
        log.info("Reserving {} units of product {}", quantity, productId);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> ResourceNotFoundException.forResource("Product", productId));

        if (!product.canBeOrdered(quantity)) {
            throw new InsufficientStockException(
                    product.getName(),
                    product.getAvailableQuantity(),
                    quantity
            );
        }

        product.reserveStock(quantity);
        productRepository.save(product);

        log.info("Reserved {} units. Available: {}", quantity, product.getAvailableQuantity());
    }

    /**
     * Release reserved stock
     */
    public void releaseReservedStock(Long productId, int quantity) {
        log.info("Releasing {} reserved units of product {}", quantity, productId);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> ResourceNotFoundException.forResource("Product",productId));

        product.releaseReservedStock(quantity);
        productRepository.save(product);
    }

    /**
     * Deduct stock (complete order)
     */
    public void deductStock(Long productId, int quantity) {
        log.info("Deducting {} units of product {}", quantity, productId);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> ResourceNotFoundException.forResource("Product", productId));

        product.deductStock(quantity);
        productRepository.save(product);

        log.info("Deducted {} units. Remaining: {}", quantity, product.getStockQuantity());
    }

    /**
     * Add stock (restock)
     */
    public void restockProduct(Long productId, int quantity) {
        log.info("Restocking product {} with {} units", productId, quantity);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> ResourceNotFoundException.forResource("Product", productId));

        product.addStock(quantity);
        productRepository.save(product);

        log.info("Restocked. New quantity: {}", product.getStockQuantity());
    }

    /**
     * Get low stock product
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "low-stock-product")
    @Override
    public List<Product> getLowStockProducts() {
        return productRepository.findLowStockProducts();
    }

    /**
     * Get product needing reorder
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "product-needing-reorder")
    @Override
    public List<Product> getProductsNeedingReorder() {
        return productRepository.findProductsNeedingReorder();
    }

    /**
     * Find product by inventory status
     */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "product-by-inventory-status", key = "#status")
    public List<Product> findByInventoryStatus(InventoryStatus status) {
        log.info("Finding product by inventory status: {}", status);
        return productRepository.findByInventoryStatus(status);
    }


    @Override
    @Transactional
    @CacheEvict(value = "product", allEntries = true)
    public void restoreStock(Long productId, Integer quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        product.setStockQuantity(product.getStockQuantity() + quantity);
        productRepository.save(product);

        log.info("Stock restored for product {}: +{} (total: {})",
                productId, quantity, product.getStockQuantity());
    }
}