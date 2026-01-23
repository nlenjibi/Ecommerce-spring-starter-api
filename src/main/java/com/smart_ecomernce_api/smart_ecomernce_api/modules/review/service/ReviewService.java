package com.smart_ecomernce_api.smart_ecomernce_api.modules.review.service;



import com.smart_ecomernce_api.smart_ecomernce_api.modules.review.dto.ReviewCreateRequest;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.review.dto.ReviewResponse;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.review.dto.ReviewUpdateRequest;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.review.entity.ProductRatingStats;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


public interface ReviewService {


    public ReviewResponse createReview(ReviewCreateRequest request, Long userId) ;
    public ReviewResponse updateReview(Long reviewId, ReviewUpdateRequest request, Long userId);
    public void deleteReview(Long reviewId, Long userId);
    public Page<ReviewResponse> getProductReviews(Long productId, Pageable pageable) ;

    public Page<ReviewResponse> getUserReviews(Long userId, Pageable pageable) ;

    public ProductRatingStats getProductRatingStats(Long productId) ;

    public void markHelpful(Long reviewId) ;

    public void markNotHelpful(Long reviewId) ;

    public ReviewResponse approveReview(Long reviewId);
}
