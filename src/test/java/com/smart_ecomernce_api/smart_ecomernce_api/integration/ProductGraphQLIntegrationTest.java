package com.smart_ecomernce_api.smart_ecomernce_api.integration;

import com.smart_ecomernce_api.smart_ecomernce_api.modules.category.entity.Category;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.category.repository.CategoryRepository;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.product.entity.Product;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.tester.AutoConfigureGraphQlTester;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.context.ActiveProfiles;

import jakarta.transaction.Transactional;
import java.math.BigDecimal;

@SpringBootTest
@AutoConfigureGraphQlTester
@ActiveProfiles("test")
class ProductGraphQLIntegrationTest {

    @Autowired
    private GraphQlTester graphQlTester;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @BeforeEach
    void setUp() {
        // Clean up database
        productRepository.deleteAll();
        categoryRepository.deleteAll();

        // Create test category
        Category category = new Category();
        category.setName("Test Category");
        category.setDescription("Category for testing");
        category.setSlug("test-category");
        Category savedCategory = categoryRepository.save(category);

        // Create test data
        Product product1 = new Product();
        product1.setName("GraphQL Test Product 1");
        product1.setDescription("First test product for GraphQL");
        product1.setPrice(new BigDecimal("99.99"));
        product1.setStockQuantity(10);
        product1.setSku("GQL-001");
        product1.setSlug("graphql-test-product-1");
        product1.setCategory(savedCategory);
        product1.setFeatured(true);
        productRepository.save(product1);

        Product product2 = new Product();
        product2.setName("GraphQL Test Product 2");
        product2.setDescription("Second test product for GraphQL");
        product2.setPrice(new BigDecimal("149.99"));
        product2.setStockQuantity(5);
        product2.setSku("GQL-002");
        product2.setSlug("graphql-test-product-2");
        product2.setCategory(savedCategory);
        product2.setFeatured(false);
        productRepository.save(product2);
    }

    @Test
    @Transactional
    void product_ShouldReturnProductById() {
        Product savedProduct = productRepository.findAll().get(0);

        String query = """
                query {
                    product(id: %d) {
                        id
                        name
                        description
                        price
                        stockQuantity
                        sku
                        isActive
                    }
                }
                """.formatted(savedProduct.getId());

        graphQlTester.document(query)
                .execute()
                .path("product")
                .matchesJson("""
                        {
                            "id": "%d",
                            "name": "GraphQL Test Product 1",
                            "description": "First test product for GraphQL",
                            "price": 99.99,
                            "stockQuantity": 10,
                            "sku": "GQL-001",
                            "isActive": true
                        }
                        """.formatted(savedProduct.getId()));
    }

    @Test
    @Transactional
    void products_ShouldReturnPaginatedProducts() {
        String query = """
                query {
                    products(pagination: {page: 0, size: 10}) {
                        content {
                            id
                            name
                            price
                            sku
                        }
                        pageInfo {
                            totalElements
                            totalPages
                            currentPage
                            pageSize
                            hasNext
                            hasPrevious
                        }
                    }
                }
                """;

        graphQlTester.document(query)
                .execute()
                .path("products.content").entityList(Object.class).hasSize(2)
                .path("products.pageInfo.totalElements").entity(Integer.class).isEqualTo(2)
                .path("products.pageInfo.totalPages").entity(Integer.class).isEqualTo(1)
                .path("products.pageInfo.currentPage").entity(Integer.class).isEqualTo(0);
    }

    @Test
    @Transactional
    void products_WithFilters_ShouldReturnFilteredResults() {
        String query = """
                query {
                    products(
                        pagination: {page: 0, size: 10},
                        filter: {minPrice: 100.00, maxPrice: 200.00}
                    ) {
                        content {
                            id
                            name
                            price
                        }
                        pageInfo {
                            totalElements
                        }
                    }
                }
                """;

        graphQlTester.document(query)
                .execute()
                .path("products.content").entityList(Object.class).hasSize(1)
                .path("products.pageInfo.totalElements").entity(Integer.class).isEqualTo(1);
    }

    @Test
    @Transactional
    void featuredProducts_ShouldReturnOnlyFeaturedProducts() {
        String query = """
                query {
                    featuredProducts(pagination: {page: 0, size: 10}) {
                        content {
                            id
                            name
                            featured
                        }
                        pageInfo {
                            totalElements
                        }
                    }
                }
                """;

        graphQlTester.document(query)
                .execute()
                .path("featuredProducts.content").entityList(Object.class).hasSize(1)
                .path("featuredProducts.pageInfo.totalElements").entity(Integer.class).isEqualTo(1);
    }

    @Test
    @Transactional
    void searchProducts_ShouldReturnMatchingProducts() {
        String query = """
                query {
                    searchProducts(search: "Product 1", pagination: {page: 0, size: 10}) {
                        content {
                            id
                            name
                        }
                        pageInfo {
                            totalElements
                        }
                    }
                }
                """;

        graphQlTester.document(query)
                .execute()
                .path("searchProducts.content").entityList(Object.class).hasSize(1)
                .path("searchProducts.pageInfo.totalElements").entity(Integer.class).isEqualTo(1);
    }

    @Test
    @Transactional
    void createProduct_ShouldCreateAndReturnNewProduct() {
        String mutation = """
                mutation {
                    createProduct(input: {
                        name: "New GraphQL Product"
                        description: "Created via GraphQL"
                        price: 79.99
                        stockQuantity: 20
                        sku: "GQL-NEW-001"
                        categoryId: 1
                        featured: false
                        isNew: true
                    }) {
                        id
                        name
                        description
                        price
                        stockQuantity
                        sku
                        isActive
                    }
                }
                """;

        graphQlTester.document(mutation)
                .execute()
                .path("createProduct")
                .matchesJson("""
                        {
                            "name": "New GraphQL Product",
                            "description": "Created via GraphQL",
                            "price": 79.99,
                            "stockQuantity": 20,
                            "sku": "GQL-NEW-001",
                            "isActive": true
                        }
                        """);

        // Verify product was created in database
        assert productRepository.findBySku("GQL-NEW-001").isPresent();
    }

    @Test
    @Transactional
    void updateProduct_ShouldUpdateAndReturnModifiedProduct() {
        Product existingProduct = productRepository.findAll().get(0);

        String mutation = """
                mutation {
                    updateProduct(id: %d, input: {
                        name: "Updated GraphQL Product"
                        price: 199.99
                        stockQuantity: 15
                    }) {
                        id
                        name
                        price
                        stockQuantity
                    }
                }
                """.formatted(existingProduct.getId());

        graphQlTester.document(mutation)
                .execute()
                .path("updateProduct")
                .matchesJson("""
                        {
                            "id": "%d",
                            "name": "Updated GraphQL Product",
                            "price": 199.99,
                            "stockQuantity": 15
                        }
                        """.formatted(existingProduct.getId()));

        // Verify product was updated in database
        Product updatedProduct = productRepository.findById(existingProduct.getId()).orElse(null);
        assert updatedProduct != null;
        assert updatedProduct.getName().equals("Updated GraphQL Product");
        assert updatedProduct.getPrice().equals(new BigDecimal("199.99"));
    }

    @Test
    @Transactional
    void deleteProduct_ShouldDeleteAndReturnTrue() {
        Product productToDelete = productRepository.findAll().get(0);

        String mutation = """
                mutation {
                    deleteProduct(id: %d)
                }
                """.formatted(productToDelete.getId());

        graphQlTester.document(mutation)
                .execute()
                .path("deleteProduct").entity(Boolean.class).isEqualTo(true);

        // Verify product was deleted from database
        assert !productRepository.existsById(productToDelete.getId());
    }

    @Test
    @Transactional
    void product_WithInvalidId_ShouldReturnError() {
        String query = """
                query {
                    product(id: 99999) {
                        id
                        name
                    }
                }
                """;

        graphQlTester.document(query)
                .execute()
                .errors()
                .expect(error -> error.getMessage().contains("not found"));
    }
}
