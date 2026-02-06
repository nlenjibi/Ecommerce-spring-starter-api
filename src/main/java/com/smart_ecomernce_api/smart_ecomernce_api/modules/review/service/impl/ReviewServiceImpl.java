package com.smart_ecomernce_api.smart_ecomernce_api.modules.review.service.impl;


import com.smart_ecomernce_api.smart_ecomernce_api.exception.InvalidDataException;
import com.smart_ecomernce_api.smart_ecomernce_api.exception.ResourceNotFoundException;
import com.smart_ecomernce_api.smart_ecomernce_api.exception.UnauthorizedException;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.review.dto.*;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.review.entity.ProductRatingStats;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.review.service.ReviewService;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.review.repository.ReviewRepository;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.review.mapper.ReviewMapper;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.product.repository.ProductRepository;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.user.repository.UserRepository;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.order.repository.OrderRepository;

import com.smart_ecomernce_api.smart_ecomernce_api.modules.product.entity.Product;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.user.entity.User;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.review.entity.Review;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;


import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;

import java.util.stream.Collectors;

/**
 * Implementation of ReviewService
 * Handles all business logic for product reviews
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final ReviewMapper reviewMapper;

    // ==================== Basic CRUD Operations ====================

    @Override
    public ReviewResponse createReview(ReviewCreateRequest request, Long userId) {
        log.info("Creating review for product {} by user {}", request.getProductId(), userId);

        // Validate user hasn't already reviewed this product
        if (reviewRepository.existsByProductIdAndUserId(request.getProductId(), userId)) {
            throw new InvalidDataException("You have already reviewed this product");
        }

        // Fetch and validate product
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> ResourceNotFoundException.forResource("Product id", request.getProductId()));

        // Fetch and validate user
        User user = userRepository.findById(userId)
                .orElseThrow(() ->  ResourceNotFoundException.forResource("Userid", userId));

        // Create review entity
        Review review = reviewMapper.toEntity(request);
        review.setProduct(product);
        review.setUser(user);

        // Check if verified purchase
        boolean hasOrdered = orderRepository.existsByUserIdAndProductId(userId, request.getProductId());
        review.setVerifiedPurchase(hasOrdered);

        // Auto-approve verified purchases, pending for others
        review.setApproved(hasOrdered);

        // Save review
        Review savedReview = reviewRepository.save(review);
        log.info("Review created successfully with ID: {}", savedReview.getId());

        return reviewMapper.toDto(savedReview);
    }

    @Override
    public ReviewResponse updateReview(Long reviewId, ReviewUpdateRequest request, Long userId) {
        log.info("Updating review {} by user {}", reviewId, userId);

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> ResourceNotFoundException.forResource("Review id", reviewId));

        // Validate ownership
        if (!review.canBeEditedBy(userId)) {
            throw new UnauthorizedException("You can only update your own reviews");
        }

        // Update review fields
        reviewMapper.updateFromDto(request, review);

        // Reset approval status if content changed significantly
        if (request.getRating() != null || request.getComment() != null) {
            review.setApproved(review.getVerifiedPurchase()); // Re-approve only if verified
        }

        Review updatedReview = reviewRepository.save(review);
        log.info("Review {} updated successfully", reviewId);

        return reviewMapper.toDto(updatedReview);
    }

    @Override
    public void deleteReview(Long reviewId, Long userId) {
        log.info("Deleting review {} by user {}", reviewId, userId);

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> ResourceNotFoundException.forResource("Review id", reviewId));

        if (!review.canBeDeletedBy(userId)) {
            throw new UnauthorizedException("You can only delete your own reviews");
        }

        review.softDelete();
        reviewRepository.save(review);
        log.info("Review {} deleted successfully", reviewId);
    }

    @Override
    @Transactional(readOnly = true)
    public ReviewResponse getReview(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> ResourceNotFoundException.forResource("Review id", reviewId));
        return reviewMapper.toDto(review);
    }

    @Override
    public ReviewResponse restoreReview(Long reviewId, Long userId) {
        log.info("Restoring review {} by user {}", reviewId, userId);

        Review review = reviewRepository.findByIdIncludingDeleted(reviewId)
                .orElseThrow(() -> ResourceNotFoundException.forResource("Review id", reviewId));

        if (!review.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("You can only restore your own reviews");
        }

        review.restore();
        Review restoredReview = reviewRepository.save(review);
        log.info("Review {} restored successfully", reviewId);

        return reviewMapper.toDto(restoredReview);
    }

    // ==================== Query Operations ====================

    @Override
    @Transactional(readOnly = true)
    public Page<ReviewResponse> getProductReviews(Long productId, Pageable pageable) {
        log.debug("Fetching reviews for product {}", productId);

        // Verify product exists
        if (!productRepository.existsById(productId)) {
            throw ResourceNotFoundException.forResource("Product id", productId);
        }

        Page<Review> reviews = reviewRepository.findByProductIdAndApproved(productId, true, pageable);
        return reviews.map(reviewMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReviewResponse> getProductReviewsWithFilters(Long productId, ReviewFilterRequest filters, Pageable pageable) {
        log.debug("Fetching filtered reviews for product {}", productId);

        Page<Review> reviews = reviewRepository.findByFilters(
                productId,
                filters.getRating(),
                filters.getVerifiedPurchase(),
                filters.getApproved(),
                filters.getWithImages(),
                filters.getDateFrom(),
                filters.getDateTo(),
                pageable
        );

        return reviews.map(reviewMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReviewResponse> getVerifiedReviews(Long productId, Pageable pageable) {
        Page<Review> reviews = reviewRepository.findByProductIdAndVerifiedPurchase(productId, true, pageable);
        return reviews.map(reviewMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReviewResponse> getUserReviews(Long userId, Pageable pageable) {
        log.debug("Fetching reviews for user {}", userId);

        if (!userRepository.existsById(userId)) {
            throw ResourceNotFoundException.forResource("User id", userId);
        }

        Page<Review> reviews = reviewRepository.findByUserId(userId, pageable);
        return reviews.map(reviewMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReviewResponse> getReviewsByRating(Long productId, Integer rating, Pageable pageable) {
        if (rating < 1 || rating > 5) {
            throw new InvalidDataException("Rating must be between 1 and 5");
        }

        Page<Review> reviews = reviewRepository.findByProductIdAndRating(productId, rating, pageable);
        return reviews.map(reviewMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewResponse> getMostHelpfulReviews(Long productId, int limit) {
        List<Review> reviews = reviewRepository.findMostHelpfulReviews(productId, limit);
        return reviews.stream()
                .map(reviewMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewResponse> getRecentReviews(Long productId, int limit) {
        List<Review> reviews = reviewRepository.findRecentReviews(productId, limit);
        return reviews.stream()
                .map(reviewMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReviewResponse> getReviewsWithImages(Long productId, Pageable pageable) {
        Page<Review> reviews = reviewRepository.findReviewsWithImages(productId, pageable);
        return reviews.map(reviewMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReviewResponse> searchReviews(Long productId, String keyword, Pageable pageable) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getProductReviews(productId, pageable);
        }

        Page<Review> reviews = reviewRepository.searchReviewsByProduct(productId, keyword.trim(), pageable);
        return reviews.map(reviewMapper::toDto);
    }

    // ==================== Statistics & Analytics ====================

    @Override
    @Transactional(readOnly = true)
    public ReviewSummaryResponse getProductRatingStats(Long productId) {
        log.debug("Fetching rating statistics for product {}", productId);

        Map<String, Object> stats = reviewRepository.getProductRatingStats(productId);
        Map<Integer, Map<String, Object>> distribution = reviewRepository.getRatingDistributionWithPercentages(productId);
        List<Map<String, Object>> topPros = reviewRepository.getMostCommonPros(productId, 5);
        List<Map<String, Object>> topCons = reviewRepository.getMostCommonCons(productId, 5);

        Double avgRating = (Double) stats.get("averageRating");
        Long totalReviews = (Long) stats.get("totalReviews");
        Long verifiedPurchases = (Long) stats.get("verifiedPurchases");
        Double verifiedPercentage = (Double) stats.get("verifiedPercentage");

        return ReviewSummaryResponse.builder()
                .totalReviews(totalReviews)
                .averageRating(avgRating)
                .verifiedPurchases(verifiedPurchases)
                .verifiedPurchasePercentage(verifiedPercentage)
                .distribution(buildRatingDistribution(distribution))
                .topPros(topPros.stream()
                        .map(m -> (String) m.get("text"))
                        .collect(Collectors.toList()))
                .topCons(topCons.stream()
                        .map(m -> (String) m.get("text"))
                        .collect(Collectors.toList()))
                .build();
    }

    private ReviewSummaryResponse.RatingDistribution buildRatingDistribution(Map<Integer, Map<String, Object>> distribution) {
        return ReviewSummaryResponse.RatingDistribution.builder()
                .fiveStars((Long) distribution.getOrDefault(5, Map.of("count", 0L)).get("count"))
                .fiveStarsPercentage((Double) distribution.getOrDefault(5, Map.of("percentage", 0.0)).get("percentage"))
                .fourStars((Long) distribution.getOrDefault(4, Map.of("count", 0L)).get("count"))
                .fourStarsPercentage((Double) distribution.getOrDefault(4, Map.of("percentage", 0.0)).get("percentage"))
                .threeStars((Long) distribution.getOrDefault(3, Map.of("count", 0L)).get("count"))
                .threeStarsPercentage((Double) distribution.getOrDefault(3, Map.of("percentage", 0.0)).get("percentage"))
                .twoStars((Long) distribution.getOrDefault(2, Map.of("count", 0L)).get("count"))
                .twoStarsPercentage((Double) distribution.getOrDefault(2, Map.of("percentage", 0.0)).get("percentage"))
                .oneStar((Long) distribution.getOrDefault(1, Map.of("count", 0L)).get("count"))
                .oneStarPercentage((Double) distribution.getOrDefault(1, Map.of("percentage", 0.0)).get("percentage"))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getUserReviewStats(Long userId) {
        return reviewRepository.getUserReviewStats(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Integer, Map<String, Object>> getRatingDistribution(Long productId) {
        return reviewRepository.getRatingDistributionWithPercentages(productId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getReviewTrends(Long productId, int months) {
        return reviewRepository.getReviewTrendsOverTime(productId, months);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getMostCommonPros(Long productId, int limit) {
        return reviewRepository.getMostCommonPros(productId, limit);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getMostCommonCons(Long productId, int limit) {
        return reviewRepository.getMostCommonCons(productId, limit);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getTopRatedProducts(int limit) {
        return reviewRepository.getTopRatedProducts(limit);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getMostReviewedProducts(int limit) {
        return reviewRepository.getMostReviewedProducts(limit);
    }

    // ==================== Voting Operations ====================

    @Override
    public void markHelpful(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> ResourceNotFoundException.forResource("Review id", reviewId));

        review.incrementHelpful();
        reviewRepository.save(review);
        log.debug("Review {} marked as helpful", reviewId);
    }

    @Override
    public void markNotHelpful(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> ResourceNotFoundException.forResource("Review id", reviewId));

        review.incrementNotHelpful();
        reviewRepository.save(review);
        log.debug("Review {} marked as not helpful", reviewId);
    }

    // ==================== Admin Operations ====================

    @Override
    public ReviewResponse approveReview(Long reviewId) {
        log.info("Approving review {}", reviewId);

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> ResourceNotFoundException.forResource("Review id", reviewId));

        review.approve();
        Review approvedReview = reviewRepository.save(review);

        return reviewMapper.toDto(approvedReview);
    }

    @Override
    public ReviewResponse rejectReview(Long reviewId, String reason) {
        log.info("Rejecting review {} with reason: {}", reviewId, reason);

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> ResourceNotFoundException.forResource("Review id", reviewId));

        review.reject(reason);
        Review rejectedReview = reviewRepository.save(review);

        return reviewMapper.toDto(rejectedReview);
    }



    @Override
    public ReviewResponse addAdminResponse(Long reviewId, AdminResponseRequest request, Long adminId) {
        log.info("Adding admin response to review {}", reviewId);

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() ->  ResourceNotFoundException.forResource("Review id", reviewId));

        review.addAdminResponse(request.getResponse(), adminId);
        Review updatedReview = reviewRepository.save(review);

        return reviewMapper.toDto(updatedReview);
    }

    @Override
    public ReviewResponse removeAdminResponse(Long reviewId) {
        log.info("Removing admin response from review {}", reviewId);

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> ResourceNotFoundException.forResource("Review id", reviewId));

        review.setAdminResponse(null);
        review.setAdminResponseAt(null);
        review.setAdminResponseBy(null);
        Review updatedReview = reviewRepository.save(review);

        return reviewMapper.toDto(updatedReview);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReviewResponse> getPendingReviews(Pageable pageable) {
        Page<Review> reviews = reviewRepository.findPendingReviews(pageable);
        return reviews.map(reviewMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReviewResponse> getFlaggedReviews(Pageable pageable) {
        Page<Review> reviews = reviewRepository.getFlaggedReviews(pageable);
        return reviews.map(reviewMapper::toDto);
    }

    @Override
    public int bulkApproveReviews(List<Long> reviewIds) {
        log.info("Bulk approving {} reviews", reviewIds.size());
        return reviewRepository.approveReviews(reviewIds);
    }

    @Override
    public int bulkRejectReviews(List<Long> reviewIds, String reason) {
        log.info("Bulk rejecting {} reviews", reviewIds.size());
        return reviewRepository.rejectReviews(reviewIds, reason);
    }

    // ==================== Utility Operations ====================

    @Override
    @Transactional(readOnly = true)
    public boolean canUserReviewProduct(Long productId, Long userId) {
        // User can review if they haven't already reviewed AND they've purchased it
        boolean hasReviewed = reviewRepository.existsByProductIdAndUserId(productId, userId);
        boolean hasPurchased = orderRepository.existsByUserIdAndProductId(userId, productId);
        return !hasReviewed && hasPurchased;
    }

    @Override
    public int updateVerificationStatusFromOrders(Long productId) {
        log.info("Updating verification status for product {}", productId);
        return reviewRepository.updateVerificationStatusFromOrders(productId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasUserPurchasedProduct(Long productId, Long userId) {
        return orderRepository.existsByUserIdAndProductId(userId, productId);
    }
}