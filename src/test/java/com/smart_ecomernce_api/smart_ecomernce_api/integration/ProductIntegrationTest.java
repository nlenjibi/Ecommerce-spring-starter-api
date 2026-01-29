package com.smart_ecomernce_api.smart_ecomernce_api.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.category.entity.Category;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.category.repository.CategoryRepository;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.order.repository.OrderItemRepository;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.product.dto.ProductCreateRequest;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.product.dto.ProductResponse;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.product.entity.Product;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.product.repository.ProductRepository;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.review.repository.ReviewRepository;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.product.repository.WishlistRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
class ProductIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private WishlistRepository wishlistRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;
    private ProductCreateRequest createRequest;
    private Category testCategory;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        // Clean up database
        wishlistRepository.deleteAll();
        orderItemRepository.deleteAll();
        reviewRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();

        // Create test category
        testCategory = Category.builder()
                .name("Test Category")
                .description("Category for testing")
                .slug("test-category")
                .build();
        testCategory = categoryRepository.save(testCategory);

        createRequest = ProductCreateRequest.builder()
                .name("Integration Test Product")
                .description("Product for integration testing")
                .price(new BigDecimal("149.99"))
                .discountedPrice(new BigDecimal("129.99"))
                .stockQuantity(25)
                .sku("INT-TEST-001")
                .categoryId(testCategory.getId())
                .build();
    }

    @Test
    void createAndRetrieveProduct_ShouldWorkEndToEnd() throws Exception {
        // Create product
        String createRequestJson = objectMapper.writeValueAsString(createRequest);

        String responseJson = mockMvc.perform(post("/v1/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createRequestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Integration Test Product"))
                .andExpect(jsonPath("$.data.price").value(149.99))
                .andExpect(jsonPath("$.data.stockQuantity").value(25))
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Extract created product ID from response
        ProductResponse createdProduct = objectMapper.readTree(responseJson)
                .get("data")
                .traverse(objectMapper)
                .readValueAs(ProductResponse.class);

        Long productId = createdProduct.getId();

        // Retrieve the created product
        mockMvc.perform(get("/v1/products/{id}", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(productId))
                .andExpect(jsonPath("$.data.name").value("Integration Test Product"))
                .andExpect(jsonPath("$.data.sku").value("INT-TEST-001"));

        // Verify product exists in database
        Product savedProduct = productRepository.findById(productId).orElse(null);
        assert savedProduct != null;
        assert savedProduct.getName().equals("Integration Test Product");
        assert savedProduct.getPrice().equals(new BigDecimal("149.99"));
    }

    @Test
    void getAllProducts_ShouldReturnPaginatedResponse() throws Exception {
        // Create multiple products
        for (int i = 1; i <= 5; i++) {
            Product product = Product.builder()
                    .name("Product " + i)
                    .description("Description " + i)
                    .price(new BigDecimal(String.valueOf(10.0 * i)))
                    .stockQuantity(10 * i)
                    .sku("SKU-" + i)
                    .slug("product-" + i)
                    .category(testCategory)
                    .build();
            productRepository.save(product);
        }

        // Get all products with pagination
        mockMvc.perform(get("/v1/products")
                .param("page", "0")
                .param("size", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content.length()").value(3))
                .andExpect(jsonPath("$.data.pageInfo.totalElements").value(5))
                .andExpect(jsonPath("$.data.pageInfo.totalPages").value(2))
                .andExpect(jsonPath("$.data.pageInfo.currentPage").value(0));
    }

    @Test
    void searchProducts_ShouldReturnFilteredResults() throws Exception {
        // Create products with different names
        Product laptop = new Product();
        laptop.setName("Gaming Laptop");
        laptop.setDescription("High performance laptop");
        laptop.setPrice(new BigDecimal("1299.99"));
        laptop.setDiscountPrice(new BigDecimal("1299.99"));
        laptop.setStockQuantity(5);
        laptop.setSku("LAPTOP-001");
        laptop.setSlug("gaming-laptop");
        laptop.setCategory(testCategory);
        laptop.setFeatured(false);
        laptop.setIsNew(false);
        productRepository.save(laptop);

        Product phone = new Product();
        phone.setName("Smartphone");
        phone.setDescription("Latest smartphone");
        phone.setPrice(new BigDecimal("699.99"));
        phone.setDiscountPrice(new BigDecimal("699.99"));
        phone.setStockQuantity(10);
        phone.setSku("PHONE-001");
        phone.setSlug("smartphone");
        phone.setCategory(testCategory);
        phone.setFeatured(false);
        phone.setIsNew(false);
        productRepository.save(phone);

        // Search for "phone"
        mockMvc.perform(get("/v1/products")
                .param("search", "phone")
                .param("categoryId", testCategory.getId().toString())
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].name").value("Smartphone"));
    }

    @Test
    void updateProduct_ShouldModifyExistingProduct() throws Exception {
        // Create a product first
        Product product = Product.builder()
                .name("Original Product")
                .description("Original description")
                .price(new BigDecimal("99.99"))
                .stockQuantity(10)
                .sku("ORIGINAL-001")
                .slug("original-product")
                .category(testCategory)
                .build();
        Product savedProduct = productRepository.save(product);

        // Update request
        String updateRequestJson = """
                {
                    "name": "Updated Product",
                    "price": 129.99,
                    "stockQuantity": 15
                }
                """;

        // Update product
        mockMvc.perform(put("/v1/products/{id}", savedProduct.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateRequestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Updated Product"))
                .andExpect(jsonPath("$.data.price").value(129.99))
                .andExpect(jsonPath("$.data.stockQuantity").value(15));

        // Verify in database
        Product updatedProduct = productRepository.findById(savedProduct.getId()).orElse(null);
        assert updatedProduct != null;
        assert updatedProduct.getName().equals("Updated Product");
        assert updatedProduct.getPrice().equals(new BigDecimal("129.99"));
    }

    @Test
    void deleteProduct_ShouldRemoveProduct() throws Exception {
        // Create a product
        Product product = Product.builder()
                .name("Product to Delete")
                .description("Will be deleted")
                .price(new BigDecimal("49.99"))
                .stockQuantity(1)
                .sku("DELETE-001")
                .slug("product-to-delete")
                .category(testCategory)
                .build();
        Product savedProduct = productRepository.save(product);

        // Delete product
        mockMvc.perform(delete("/v1/products/{id}", savedProduct.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Verify product is deleted
        assert !productRepository.existsById(savedProduct.getId());
    }

    @Test
    void getNonExistentProduct_ShouldReturnNotFound() throws Exception {
        mockMvc.perform(get("/v1/products/99999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void createProduct_WithInvalidData_ShouldReturnBadRequest() throws Exception {
        String invalidRequestJson = """
                {
                    "name": "",
                    "price": -10.00,
                    "stockQuantity": -5
                }
                """;

        mockMvc.perform(post("/v1/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidRequestJson))
                .andExpect(status().isBadRequest());
    }
}
