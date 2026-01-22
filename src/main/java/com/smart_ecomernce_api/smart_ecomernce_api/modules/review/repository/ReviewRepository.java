package com.smart_ecomernce_api.smart_ecomernce_api.modules.review.repository;

import com.smart_ecomernce_api.Smart_ecommerce_api.modules.review.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    // Find reviews by product
    Page<Review> findByProductIdAndApprovedTrue(Long productId, Pageable pageable);

    // Find reviews by user
    Page<Review> findByUserId(Long userId, Pageable pageable);

    // Find verified purchase reviews
    Page<Review> findByProductIdAndVerifiedPurchaseTrueAndApprovedTrue(
            Long productId, Pageable pageable);

    // Find by rating
    Page<Review> findByProductIdAndRatingAndApprovedTrue(
            Long productId, Integer rating, Pageable pageable);

    // Check if user already reviewed product
    boolean existsByProductIdAndUserId(Long productId, Long userId);

    Optional<Review> findByProductIdAndUserId(Long productId, Long userId);

    // Count reviews by product
    long countByProductIdAndApprovedTrue(Long productId);

    // Get average rating
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.product.id = :productId AND r.approved = true")
    Double getAverageRatingByProductId(@Param("productId") Long productId);

    // Get rating distribution
    @Query("SELECT r.rating, COUNT(r) FROM Review r WHERE r.product.id = :productId " +
            "AND r.approved = true GROUP BY r.rating ORDER BY r.rating DESC")
    List<Object[]> getRatingDistribution(@Param("productId") Long productId);

    // Find pending reviews (for admin)
    Page<Review> findByApprovedFalse(Pageable pageable);

    // Find most helpful reviews
    @Query("SELECT r FROM Review r WHERE r.product.id = :productId AND r.approved = true " +
            "ORDER BY r.helpfulCount DESC")
    List<Review> findMostHelpfulReviews(@Param("productId") Long productId, Pageable pageable);
}
