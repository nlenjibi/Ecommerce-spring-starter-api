package com.smart_ecomernce_api.smart_ecomernce_api.modules.review.repository;

import com.smart_ecomernce_api.smart_ecomernce_api.modules.review.entity.ProductRatingStats;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.review.entity.Review;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Repository interface for Review entity operations
 * Provides comprehensive CRUD and query methods for product reviews
 */
public interface ReviewRepository {

    // ==================== Basic CRUD Operations ====================

    /**
     * Save a new review or update existing one
     */
    Review save(Review review);

    /**
     * Find review by ID (excluding deleted)
     */
    Optional<Review> findById(Long id);

    /**
     * Find review by ID including deleted
     */
    Optional<Review> findByIdIncludingDeleted(Long id);

    /**
     * Delete review by ID (soft delete)
     */
    boolean deleteById(Long id);

    /**
     * Hard delete review by ID
     */
    boolean hardDeleteById(Long id);

    /**
     * Restore soft-deleted review
     */
    boolean restoreById(Long id);

    /**
     * Check if review exists
     */
    boolean existsById(Long id);

    /**
     * Check if user has reviewed a product
     */
    boolean existsByProductIdAndUserId(Long productId, Long userId);

    // ==================== Query Methods ====================

    /**
     * Find all reviews with pagination
     */
    Page<Review> findAll(Pageable pageable);

    /**
     * Find reviews by product ID
     */
    Page<Review> findByProductId(Long productId, Pageable pageable);

    /**
     * Find approved reviews by product ID
     */
    Page<Review> findByProductIdAndApproved(Long productId, Boolean approved, Pageable pageable);

    /**
     * Find verified purchase reviews for a product
     */
    Page<Review> findByProductIdAndVerifiedPurchase(Long productId, Boolean verifiedPurchase, Pageable pageable);

    /**
     * Find reviews by user ID
     */
    Page<Review> findByUserId(Long userId, Pageable pageable);

    /**
     * Find reviews by rating
     */
    Page<Review> findByProductIdAndRating(Long productId, Integer rating, Pageable pageable);

    /**
     * Find pending reviews (not approved)
     */
    Page<Review> findPendingReviews(Pageable pageable);

    /**
     * Find most helpful reviews for a product
     */
    List<Review> findMostHelpfulReviews(Long productId, int limit);

    /**
     * Find recent reviews for a product
     */
    List<Review> findRecentReviews(Long productId, int limit);

    /**
     * Find reviews with images
     */
    Page<Review> findReviewsWithImages(Long productId, Pageable pageable);

    /**
     * Find reviews by date range
     */
    Page<Review> findByProductIdAndDateRange(Long productId, LocalDateTime from, LocalDateTime to, Pageable pageable);

    /**
     * Find reviews by multiple filters
     */
    Page<Review> findByFilters(Long productId, Integer rating, Boolean verifiedPurchase,
                               Boolean approved, Boolean withImages,
                               LocalDateTime dateFrom, LocalDateTime dateTo,
                               Pageable pageable);

    // ==================== Count Methods ====================

    /**
     * Count total reviews
     */
    long count();

    /**
     * Count reviews by product
     */
    long countByProductId(Long productId);

    /**
     * Count approved reviews by product
     */
    long countByProductIdAndApproved(Long productId, Boolean approved);

    /**
     * Count verified purchase reviews
     */
    long countByProductIdAndVerifiedPurchase(Long productId, Boolean verifiedPurchase);

    /**
     * Count pending reviews
     */
    long countPendingReviews();

    /**
     * Count reviews by user
     */
    long countByUserId(Long userId);

    /**
     * Count reviews by rating
     */
    long countByProductIdAndRating(Long productId, Integer rating);

    // ==================== Statistics & Analytics ====================

    /**
     * Get average rating for a product
     */
    Double getAverageRatingByProductId(Long productId);

    /**
     * Get rating distribution for a product
     * Returns map with rating (1-5) as key and count as value
     */
    Map<Integer, Long> getRatingDistribution(Long productId);

    /**
     * Get rating distribution with percentages
     */
    Map<Integer, Map<String, Object>> getRatingDistributionWithPercentages(Long productId);

    /**
     * Get product rating statistics
     */
    Map<String, Object> getProductRatingStats(Long productId);

    /**
     * Get user review statistics
     */
    Map<String, Object> getUserReviewStats(Long userId);

    /**
     * Get top rated products
     */
    List<Map<String, Object>> getTopRatedProducts(int limit);

    /**
     * Get most reviewed products
     */
    List<Map<String, Object>> getMostReviewedProducts(int limit);

    /**
     * Get review trends over time (grouped by month)
     */
    List<Map<String, Object>> getReviewTrendsOverTime(Long productId, int months);

    /**
     * Get average rating trend over time
     */
    List<Map<String, Object>> getAverageRatingTrend(Long productId, int months);

    // ==================== Update Operations ====================

    /**
     * Approve a review
     */
    boolean approveReview(Long reviewId);

    /**
     * Reject a review with reason
     */
    boolean rejectReview(Long reviewId, String reason);

    /**
     * Increment helpful count
     */
    boolean incrementHelpfulCount(Long reviewId);

    /**
     * Increment not helpful count
     */
    boolean incrementNotHelpfulCount(Long reviewId);

    /**
     * Add admin response to review
     */
    boolean addAdminResponse(Long reviewId, String response, Long adminId);

    /**
     * Remove admin response
     */
    boolean removeAdminResponse(Long reviewId);

    /**
     * Update review verification status
     */
    boolean updateVerificationStatus(Long reviewId, Boolean verified);

    // ==================== Bulk Operations ====================

    /**
     * Delete all reviews by product ID
     */
    int deleteByProductId(Long productId);

    /**
     * Delete all reviews by user ID
     */
    int deleteByUserId(Long userId);

    /**
     * Approve multiple reviews
     */
    int approveReviews(List<Long> reviewIds);

    /**
     * Reject multiple reviews
     */
    int rejectReviews(List<Long> reviewIds, String reason);

    /**
     * Bulk update verification status based on orders
     */
    int updateVerificationStatusFromOrders(Long productId);

    // ==================== Search & Filter ====================

    /**
     * Search reviews by keyword in title or comment
     */
    Page<Review> searchReviews(String keyword, Pageable pageable);

    /**
     * Search reviews for a specific product
     */
    Page<Review> searchReviewsByProduct(Long productId, String keyword, Pageable pageable);

    /**
     * Get reviews that need moderation
     */
    Page<Review> getReviewsNeedingModeration(Pageable pageable);

    /**
     * Get flagged/reported reviews
     */
    Page<Review> getFlaggedReviews(Pageable pageable);

    // ==================== Analytics Methods ====================

    /**
     * Get most common pros for a product
     */
    List<Map<String, Object>> getMostCommonPros(Long productId, int limit);

    /**
     * Get most common cons for a product
     */
    List<Map<String, Object>> getMostCommonCons(Long productId, int limit);

    /**
     * Get review velocity (reviews per day/week/month)
     */
    Map<String, Object> getReviewVelocity(Long productId);

    /**
     * Get verified vs unverified purchase statistics
     */
    Map<String, Object> getVerificationStats(Long productId);

    /**
     * Get helpfulness statistics
     */
    Map<String, Object> getHelpfulnessStats(Long productId);
}