package com.smart_ecomernce_api.smart_ecomernce_api.modules.product.entity;

import com.smart_ecomernce_api.smart_ecomernce_api.common.base.BaseEntity;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.category.entity.Category;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.order.entity.OrderItem;
import com.smart_ecomernce_api.smart_ecomernce_api.validator.ValidPriceRange;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;
import org.hibernate.annotations.Formula;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "products",
        indexes = {
                @Index(name = "idx_product_slug", columnList = "slug"),
                @Index(name = "idx_product_name", columnList = "name"),
                @Index(name = "idx_product_category", columnList = "category_id"),
                @Index(name = "idx_product_sku", columnList = "sku"),
                @Index(name = "idx_product_status", columnList = "inventory_status"),
                @Index(name = "idx_product_stock", columnList = "stock_quantity"),
                @Index(name = "idx_product_featured", columnList = "featured"),
                @Index(name = "idx_product_active", columnList = "is_active")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ValidPriceRange
public class Product extends BaseEntity {

    @Column(name = "name", nullable = false, length = 200)
    @NotBlank(message = "Product name cannot be blank")
    private String name;

    @Column(name = "description", columnDefinition = "TEXT", length = 500)
    private String description;

    @Column(name = "slug", nullable = false, unique = true, length = 250)
    @NotBlank(message = "Product slug cannot be blank")
    private String slug;

    @Column(name = "sku", unique = true, length = 100)
    private String sku;

    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    @NotNull(message = "Price is required")
    @Positive(message = "Price must be positive")
    private BigDecimal price;

    @Column(name = "discount_price", precision = 10, scale = 2)
    @Positive(message = "Discount price must be positive")
    private BigDecimal discountPrice;

    @Column(name = "cost_price", precision = 10, scale = 2)
    @Positive(message = "Cost price must be positive")
    private BigDecimal costPrice;


    /**
     * Current available stock quantity
     */
    @Column(name = "stock_quantity", nullable = false)
    @NotNull(message = "Stock quantity is required")
    @Min(value = 0, message = "Stock quantity cannot be negative")
    @Builder.Default
    private Integer stockQuantity = 0;

    /**
     * Quantity reserved for pending/processing orders
     */
    @Column(name = "reserved_quantity", nullable = false)
    @Min(value = 0, message = "Reserved quantity cannot be negative")
    @Builder.Default
    private Integer reservedQuantity = 0;

    /**
     * Threshold for low stock warning
     */
    @Column(name = "low_stock_threshold", nullable = false)
    @Min(value = 0, message = "Low stock threshold cannot be negative")
    @Builder.Default
    private Integer lowStockThreshold = 10;

    /**
     * Reorder point - when to restock
     */
    @Column(name = "reorder_point", nullable = false)
    @Min(value = 0, message = "Reorder point cannot be negative")
    @Builder.Default
    private Integer reorderPoint = 5;

    /**
     * Optimal reorder quantity
     */
    @Column(name = "reorder_quantity")
    @Positive(message = "Reorder quantity must be positive")
    private Integer reorderQuantity;

    /**
     * Maximum stock capacity
     */
    @Column(name = "max_stock_quantity")
    @Positive(message = "Max stock quantity must be positive")
    private Integer maxStockQuantity;

    /**
     * Current inventory status (calculated or manually set)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "inventory_status", nullable = false, length = 20)
    @Builder.Default
    private InventoryStatus inventoryStatus = InventoryStatus.IN_STOCK;

    /**
     * Track inventory automatically based on stock levels
     */
    @Column(name = "track_inventory")
    @Builder.Default
    private Boolean trackInventory = true;

    /**
     * Allow orders when out of stock (backorder)
     */
    @Column(name = "allow_backorder")
    @Builder.Default
    private Boolean allowBackorder = false;

    /**
     * Expected restock date
     */
    @Column(name = "expected_restock_date")
    private LocalDateTime expectedRestockDate;

    /**
     * Last restocked date
     */
    @Column(name = "last_restocked_at")
    private LocalDateTime lastRestockedAt;

    @Column(name = "featured")
    @Builder.Default
    private Boolean featured = false;

    @Column(name = "is_new")
    @Builder.Default
    private Boolean isNew = false;

    @Column(name = "is_bestseller")
    @Builder.Default
    private Boolean isBestseller = false;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "thumbnail_url")
    private String thumbnailUrl;

    @ElementCollection
    @CollectionTable(name = "product_additional_images", joinColumns = @JoinColumn(name = "product_id"))
    @Column(name = "image_url")
    @Builder.Default
    private List<String> additionalImages = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    @NotNull(message = "Category is required")
    private Category category;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ProductImage> images = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> orderItems;

    /**
     * Available quantity (stock - reserved)
     * Calculated by database
     */
    @Formula("stock_quantity - reserved_quantity")
    private Integer availableQuantity;

    /**
     * Check if product is in stock
     */
    public boolean isInStock() {
        if (!trackInventory) {
            return true; // Always in stock if not tracking inventory
        }
        return getAvailableQuantity() > 0;
    }

    /**
     * Check if stock is low
     */
    public boolean isLowStock() {
        if (!trackInventory) {
            return false;
        }
        return getAvailableQuantity() > 0 &&
                getAvailableQuantity() <= lowStockThreshold;
    }

    /**
     * Check if out of stock
     */
    public boolean isOutOfStock() {
        if (!trackInventory) {
            return false;
        }
        return getAvailableQuantity() <= 0;
    }

    /**
     * Check if needs reorder
     */
    public boolean needsReorder() {
        if (!trackInventory) {
            return false;
        }
        return stockQuantity <= reorderPoint;
    }

    /**
     * Get available quantity (stock - reserved)
     */
    public Integer getAvailableQuantity() {
        if (availableQuantity != null) {
            return availableQuantity; // Use Formula calculated value
        }
        return stockQuantity - reservedQuantity;
    }

    /**
     * Get effective selling price (considers discount)
     */
    public BigDecimal getEffectivePrice() {
        return discountPrice != null && discountPrice.compareTo(price) < 0
                ? discountPrice
                : price;
    }

    /**
     * Check if product has discount
     */
    public boolean hasDiscount() {
        return discountPrice != null &&
                discountPrice.compareTo(BigDecimal.ZERO) > 0 &&
                discountPrice.compareTo(price) < 0;
    }

    /**
     * Calculate discount percentage
     */
    public BigDecimal getDiscountPercentage() {
        if (!hasDiscount()) {
            return BigDecimal.ZERO;
        }

        BigDecimal discount = price.subtract(discountPrice);
        return discount.divide(price, 2, BigDecimal.ROUND_HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    /**
     * Calculate profit margin
     */
    public BigDecimal getProfitMargin() {
        if (costPrice == null || costPrice.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal profit = getEffectivePrice().subtract(costPrice);
        return profit.divide(costPrice, 2, BigDecimal.ROUND_HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    /**
     * Reserve stock for order
     */
    public void reserveStock(int quantity) {
        if (!trackInventory) {
            return; // Don't reserve if not tracking
        }

        if (getAvailableQuantity() < quantity) {
            throw new IllegalStateException(
                    "Insufficient stock. Available: " + getAvailableQuantity() +
                            ", Requested: " + quantity
            );
        }

        this.reservedQuantity += quantity;
        updateInventoryStatus();
    }

    /**
     * Release reserved stock (order cancelled)
     */
    public void releaseReservedStock(int quantity) {
        if (!trackInventory) {
            return;
        }

        this.reservedQuantity = Math.max(0, this.reservedQuantity - quantity);
        updateInventoryStatus();
    }

    /**
     * Deduct stock (order completed)
     */
    public void deductStock(int quantity) {
        if (!trackInventory) {
            return;
        }

        // First release from reserved
        int releaseAmount = Math.min(quantity, this.reservedQuantity);
        this.reservedQuantity -= releaseAmount;

        // Then deduct from stock
        if (this.stockQuantity < quantity) {
            throw new IllegalStateException(
                    "Insufficient stock to deduct. Stock: " + this.stockQuantity +
                            ", Requested: " + quantity
            );
        }

        this.stockQuantity -= quantity;
        updateInventoryStatus();
    }

    /**
     * Add stock (restock)
     */
    public void addStock(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }

        this.stockQuantity += quantity;
        this.lastRestockedAt = LocalDateTime.now();
        updateInventoryStatus();
    }

    /**
     * Set stock quantity (inventory adjustment)
     */
    public void setStockQuantity(Integer quantity) {
        if (quantity < 0) {
            throw new IllegalArgumentException("Stock quantity cannot be negative");
        }

        this.stockQuantity = quantity;
        updateInventoryStatus();
    }

    /**
     * Update inventory status based on current stock
     */
    public void updateInventoryStatus() {
        if (!trackInventory) {
            this.inventoryStatus = InventoryStatus.IN_STOCK;
            return;
        }

        int available = getAvailableQuantity();

        if (available <= 0) {
            if (allowBackorder) {
                this.inventoryStatus = InventoryStatus.BACKORDER;
            } else {
                this.inventoryStatus = InventoryStatus.OUT_OF_STOCK;
            }
        } else if (available <= lowStockThreshold) {
            this.inventoryStatus = InventoryStatus.LOW_STOCK;
        } else {
            this.inventoryStatus = InventoryStatus.IN_STOCK;
        }
    }

    /**
     * Check if product can be ordered
     */
    public boolean canBeOrdered(int quantity) {
        if (!isActive) {
            return false;
        }

        if (inventoryStatus == InventoryStatus.DISCONTINUED) {
            return false;
        }

        if (!trackInventory) {
            return true; // Always orderable if not tracking inventory
        }

        if (allowBackorder) {
            return true; // Can order even if out of stock
        }

        return getAvailableQuantity() >= quantity;
    }

    /**
     * Get inventory status message
     */
    public String getInventoryStatusMessage() {
        return switch (inventoryStatus) {
            case IN_STOCK -> "In Stock (" + getAvailableQuantity() + " available)";
            case LOW_STOCK -> "Low Stock - Only " + getAvailableQuantity() + " left!";
            case OUT_OF_STOCK -> expectedRestockDate != null
                    ? "Out of Stock - Expected: " + expectedRestockDate.toLocalDate()
                    : "Out of Stock";
            case BACKORDER -> "Available for Backorder";
            case PRE_ORDER -> "Available for Pre-Order";
            case DISCONTINUED -> "Product Discontinued";
        };
    }

    @PrePersist
    @PreUpdate
    protected void validateAndUpdate() {
        // Update inventory status before save
        updateInventoryStatus();

        // Validate reserved quantity
        if (reservedQuantity > stockQuantity) {
            throw new IllegalStateException(
                    "Reserved quantity cannot exceed stock quantity"
            );
        }

        // Validate discount price
        if (discountPrice != null && discountPrice.compareTo(price) >= 0) {
            throw new IllegalArgumentException(
                    "Discount price must be less than regular price"
            );
        }
    }

    public void addImage(ProductImage image) {
        if (image != null) {
            image.setProduct(this);
            this.images.add(image);
        }
    }

    public void removeImage(ProductImage image) {
        if (image != null) {
            image.setProduct(null);
            this.images.remove(image);
        }
    }
}

