package com.smart_ecomernce_api.smart_ecomernce_api.modules.review.service.impl;

import com.smart_ecomernce_api.Smart_ecommerce_api.exception.InvalidDataException;
import com.smart_ecomernce_api.Smart_ecommerce_api.exception.ResourceNotFoundException;
import com.smart_ecomernce_api.Smart_ecommerce_api.modules.order.repository.OrderRepository;
import com.smart_ecomernce_api.Smart_ecommerce_api.modules.product.entity.Product;
import com.smart_ecomernce_api.Smart_ecommerce_api.modules.product.repository.ProductRepository;
import com.smart_ecomernce_api.Smart_ecommerce_api.modules.review.dto.ReviewCreateRequest;
import com.smart_ecomernce_api.Smart_ecommerce_api.modules.review.dto.ReviewResponse;
import com.smart_ecomernce_api.Smart_ecommerce_api.modules.review.dto.ReviewUpdateRequest;
import com.smart_ecomernce_api.Smart_ecommerce_api.modules.review.entity.ProductRatingStats;
import com.smart_ecomernce_api.Smart_ecommerce_api.modules.review.entity.Review;
import com.smart_ecomernce_api.Smart_ecommerce_api.modules.review.mapper.ReviewMapper;
import com.smart_ecomernce_api.Smart_ecommerce_api.modules.review.repository.ReviewRepository;
import com.smart_ecomernce_api.Smart_ecommerce_api.modules.review.service.ReviewService;
import com.smart_ecomernce_api.Smart_ecommerce_api.modules.user.entity.User;
import com.smart_ecomernce_api.Smart_ecommerce_api.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    @Override
    @CacheEvict(value = {"reviews", "productRatingStats"}, allEntries = true)
    public ReviewResponse createReview(ReviewCreateRequest request, Long userId) {
        log.info("Creating review for product {} by user {}", request.getProductId(), userId);

        // Check if user already reviewed this product
        if (reviewRepository.existsByProductIdAndUserId(request.getProductId(), userId)) {
            throw new InvalidDataException("You have already reviewed this product");
        }

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> ResourceNotFoundException.forResource("Product", request.getProductId()));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> ResourceNotFoundException.forResource("User", userId));

        Review review = reviewMapper.toEntity(request);
        review.setProduct(product);
        review.setUser(user);

        // Check if verified purchase
        boolean verifiedPurchase = orderRepository.existsByUserIdAndProductId(userId, request.getProductId());
        review.setVerifiedPurchase(verifiedPurchase);

        // Auto-approve verified purchases or set pending
        review.setApproved(verifiedPurchase);

        Review saved = reviewRepository.save(review);
        log.info("Review created with ID: {}", saved.getId());

        return reviewMapper.toDto(saved);
    }


    @Override
    @CacheEvict(value = {"reviews", "productRatingStats"}, allEntries = true)
    public ReviewResponse updateReview(Long reviewId, ReviewUpdateRequest request, Long userId) {
        log.info("Updating review {} by user {}", reviewId, userId);

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> ResourceNotFoundException.forResource("Review", reviewId));

        // Check ownership
        if (!review.getUser().getId().equals(userId)) {
            throw new InvalidDataException("You can only update your own reviews");
        }

        reviewMapper.update(request, review);
        Review updated = reviewRepository.save(review);

        return reviewMapper.toDto(updated);
    }


    @Override
    @CacheEvict(value = {"reviews", "productRatingStats"}, allEntries = true)
    public void deleteReview(Long reviewId, Long userId) {
        log.info("Deleting review {} by user {}", reviewId, userId);

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> ResourceNotFoundException.forResource("Review", reviewId));

        if (!review.getUser().getId().equals(userId)) {
            throw new InvalidDataException("You can only delete your own reviews");
        }

        reviewRepository.delete(review);
    }

    /**
     * Get reviews by product
     */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "reviews", key = "#productId + '_' + #pageable.pageNumber + '_' + #pageable.pageSize")
    public Page<ReviewResponse> getProductReviews(Long productId, Pageable pageable) {
        return reviewRepository.findByProductIdAndApprovedTrue(productId, pageable)
                .map(reviewMapper::toDto);
    }

    /**
     * Get reviews by user
     */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "reviews", key = "'user_' + #userId + '_' + #pageable.pageNumber + '_' + #pageable.pageSize")
    public Page<ReviewResponse> getUserReviews(Long userId, Pageable pageable) {
        return reviewRepository.findByUserId(userId, pageable)
                .map(reviewMapper::toDto);
    }

    /**
     * Get product rating statistics
     */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "productRatingStats", key = "#productId")
    public ProductRatingStats getProductRatingStats(Long productId) {
        Double avgRating = reviewRepository.getAverageRatingByProductId(productId);
        Long totalReviews = reviewRepository.countByProductIdAndApprovedTrue(productId);
        List<Object[]> distribution = reviewRepository.getRatingDistribution(productId);

        Map<Integer, Long> ratingMap = new HashMap<>();
        for (Object[] row : distribution) {
            ratingMap.put((Integer) row[0], (Long) row[1]);
        }

        return ProductRatingStats.builder()
                .productId(productId)
                .averageRating(avgRating != null ? avgRating : 0.0)
                .totalReviews(totalReviews)
                .distribution(ProductRatingStats.RatingDistribution.builder()
                        .fiveStars(ratingMap.getOrDefault(5, 0L))
                        .fourStars(ratingMap.getOrDefault(4, 0L))
                        .threeStars(ratingMap.getOrDefault(3, 0L))
                        .twoStars(ratingMap.getOrDefault(2, 0L))
                        .oneStar(ratingMap.getOrDefault(1, 0L))
                        .build())
                .build();
    }

    /**
     * Mark review as helpful
     */
    @Override
    @CacheEvict(value = {"reviews", "productRatingStats"}, allEntries = true)
    public void markHelpful(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> ResourceNotFoundException.forResource("Review", reviewId));
        review.incrementHelpful();
        reviewRepository.save(review);
    }

    /**
     * Mark review as not helpful
     */
    @Override
    @CacheEvict(value = {"reviews", "productRatingStats"}, allEntries = true)
    public void markNotHelpful(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> ResourceNotFoundException.forResource("Review", reviewId));
        review.incrementNotHelpful();
        reviewRepository.save(review);
    }

    /**
     * Approve review (Admin)
     */
    @Override
    @CacheEvict(value = {"reviews", "productRatingStats"}, allEntries = true)
    public ReviewResponse approveReview(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> ResourceNotFoundException.forResource("Review", reviewId));
        review.setApproved(true);
        return reviewMapper.toDto(reviewRepository.save(review));
    }
}
