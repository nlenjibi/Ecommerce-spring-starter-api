package com.smart_ecomernce_api.smart_ecomernce_api.modules.review.entity;

import com.smart_ecomernce_api.smart_ecomernce_api.common.base.BaseEntity;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.product.entity.Product;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.user.entity.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "reviews", indexes = {
        @Index(name = "idx_review_product", columnList = "product_id"),
        @Index(name = "idx_review_user", columnList = "user_id"),
        @Index(name = "idx_review_rating", columnList = "rating"),
        @Index(name = "idx_review_verified", columnList = "verified_purchase"),
        @Index(name = "idx_review_approved", columnList = "approved")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Review extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    @NotNull(message = "Product is required")
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull(message = "User is required")
    private User user;

    @Column(name = "rating", nullable = false)
    @NotNull(message = "Rating is required")
    @Min(value = 1, message = "Rating must be at least 1")
    @Max(value = 5, message = "Rating must be at most 5")
    private Integer rating;

    @Column(name = "title", length = 200)
    @Size(max = 200, message = "Title must not exceed 200 characters")
    private String title;

    @Column(name = "comment", columnDefinition = "TEXT")
    @NotBlank(message = "Review comment is required")
    @Size(min = 10, max = 2000, message = "Comment must be between 10 and 2000 characters")
    private String comment;

    @Column(name = "verified_purchase")
    @Builder.Default
    private Boolean verifiedPurchase = false;

    @Column(name = "approved")
    @Builder.Default
    private Boolean approved = false;

    @Column(name = "helpful_count")
    @Builder.Default
    private Integer helpfulCount = 0;

    @Column(name = "not_helpful_count")
    @Builder.Default
    private Integer notHelpfulCount = 0;

    @ElementCollection
    @CollectionTable(name = "review_images", joinColumns = @JoinColumn(name = "review_id"))
    @Column(name = "image_url")
    @Builder.Default
    private List<String> images = new ArrayList<>();

    // Pros and Cons
    @ElementCollection
    @CollectionTable(name = "review_pros", joinColumns = @JoinColumn(name = "review_id"))
    @Column(name = "pro")
    @Builder.Default
    private List<String> pros = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "review_cons", joinColumns = @JoinColumn(name = "review_id"))
    @Column(name = "con")
    @Builder.Default
    private List<String> cons = new ArrayList<>();

    // Admin response
    @Column(name = "admin_response", columnDefinition = "TEXT")
    private String adminResponse;

    public boolean isHelpful() {
        return helpfulCount > notHelpfulCount;
    }

    public void incrementHelpful() {
        this.helpfulCount++;
    }

    public void incrementNotHelpful() {
        this.notHelpfulCount++;
    }
}
