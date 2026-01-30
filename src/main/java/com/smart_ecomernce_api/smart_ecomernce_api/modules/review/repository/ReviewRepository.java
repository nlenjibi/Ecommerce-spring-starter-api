package com.smart_ecomernce_api.smart_ecomernce_api.modules.review.repository;

import com.smart_ecomernce_api.smart_ecomernce_api.modules.review.entity.ProductRatingStats;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.review.entity.Review;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * JDBC-based Review Repository Interface
 * Handles product reviews, ratings, and statistics
 */
public interface ReviewRepository {

    /**
     * Save a new review
     */
    Review save(Review review);

    /**
     * Update an existing review
     */
    Review update(Review review);

    /**
     * Find review by ID
     */
    Optional<Review> findById(Long id);

    /**
     * Find all reviews
     */
    List<Review> findAll();

    /**
     * Find all reviews with pagination
     */
    List<Review> findAll(int page, int size);

    /**
     * Find reviews by product ID
     */
    List<Review> findByProductId(Long productId, int page, int size);

    /**
     * Find approved reviews by product ID
     */
    List<Review> findByProductIdAndApprovedTrue(Long productId, int page, int size);

    /**
     * Find reviews by user ID
     */
    List<Review> findByUserId(Long userId, int page, int size);

    /**
     * Find pending reviews (not approved)
     */
    List<Review> findPendingReviews(int page, int size);

    /**
     * Find verified purchase reviews for a product
     */
    List<Review> findVerifiedPurchaseReviews(Long productId, int page, int size);

    /**
     * Find most helpful reviews for a product
     */
    List<Review> findMostHelpfulReviews(Long productId, int limit);

    /**
     * Find reviews by rating
     */
    List<Review> findByProductIdAndRating(Long productId, Integer rating, int page, int size);

    /**
     * Find recent reviews for a product
     */
    List<Review> findRecentReviews(Long productId, int limit);

    /**
     * Find reviews with images
     */
    List<Review> findReviewsWithImages(Long productId, int page, int size);

    /**
     * Check if user has reviewed a product
     */
    boolean existsByProductIdAndUserId(Long productId, Long userId);

    /**
     * Check if review exists by ID
     */
    boolean existsById(Long id);

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
    long countByProductIdAndApprovedTrue(Long productId);

    /**
     * Count pending reviews
     */
    long countPendingReviews();

    /**
     * Count verified purchase reviews
     */
    long countVerifiedPurchaseReviews(Long productId);

    /**
     * Get average rating for a product
     */
    Double getAverageRatingByProductId(Long productId);

    /**
     * Get rating distribution for a product
     */
    Map<Integer, Long> getRatingDistribution(Long productId);

    /**
     * Get product rating statistics
     */
    ProductRatingStats getProductRatingStats(Long productId);

    /**
     * Approve a review
     */
    boolean approveReview(Long reviewId);

    /**
     * Reject a review
     */
    boolean rejectReview(Long reviewId);

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
    boolean addAdminResponse(Long reviewId, String response);

    /**
     * Delete review by ID
     */
    boolean deleteById(Long id);

    /**
     * Delete all reviews by product ID
     */
    int deleteByProductId(Long productId);

    /**
     * Delete all reviews by user ID
     */
    int deleteByUserId(Long userId);

    /**
     * Get top rated products (by average rating)
     */
    List<Map<String, Object>> getTopRatedProducts(int limit);

    /**
     * Get most reviewed products
     */
    List<Map<String, Object>> getMostReviewedProducts(int limit);

    /**
     * Get user review statistics
     */
    Map<String, Object> getUserReviewStats(Long userId);
}