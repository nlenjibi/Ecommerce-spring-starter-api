package com.smart_ecomernce_api.smart_ecomernce_api.modules.review.controller;


import com.smart_ecomernce_api.smart_ecomernce_api.common.response.ApiResponse;
import com.smart_ecomernce_api.smart_ecomernce_api.common.response.PaginatedResponse;
import com.smart_ecomernce_api.smart_ecomernce_api.exception.BadRequestException;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.review.dto.*;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.review.entity.ProductRatingStats;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.review.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;

import org.springframework.web.bind.annotation.*;


import java.util.List;
import java.util.Map;

/**
 * REST Controller for Product Review Management
 * Provides comprehensive endpoints for creating, managing, and analyzing product reviews
 */
@RestController
@RequestMapping("v1/reviews")
@RequiredArgsConstructor
@Tag(name = "Product Reviews", description = "Comprehensive APIs for managing product reviews, ratings, and analytics")
public class ReviewController {

    private final ReviewService reviewService;

    // ==================== Basic CRUD Operations ====================

    @PostMapping
    @Operation(
            summary = "Create a product review",
            description = "Create a new review for a product. Users can only review products they've purchased and can only submit one review per product."
    )
    public ResponseEntity<ApiResponse<ReviewResponse>> createReview(
            @Valid @RequestBody ReviewCreateRequest request,
            @RequestParam(required = false) Long userId) {

        ReviewResponse response = reviewService.createReview(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Review created successfully", response));
    }

    @PutMapping("/{reviewId}")
    @Operation(
            summary = "Update your review",
            description = "Update an existing review. Users can only update their own reviews."
    )
    public ResponseEntity<ApiResponse<ReviewResponse>> updateReview(
            @PathVariable @Positive Long reviewId,
            @Valid @RequestBody ReviewUpdateRequest request,
            @RequestParam(required = false) Long userId) {

        ReviewResponse response = reviewService.updateReview(reviewId, request, userId);
        return ResponseEntity.ok(ApiResponse.success("Review updated successfully", response));
    }

    @DeleteMapping("/{reviewId}")
    @Operation(
            summary = "Delete your review",
            description = "Soft delete a review. Users can only delete their own reviews."
    )
    public ResponseEntity<ApiResponse<Void>> deleteReview(
            @PathVariable @Positive Long reviewId,
            @RequestParam(required = false) Long userId) {

        reviewService.deleteReview(reviewId, userId);
        return ResponseEntity.ok(ApiResponse.success("Review deleted successfully", null));
    }

    @GetMapping("/{reviewId}")
    @Operation(
            summary = "Get review by ID",
            description = "Retrieve detailed information about a specific review"
    )
    public ResponseEntity<ApiResponse<ReviewResponse>> getReview(
            @PathVariable @Positive Long reviewId) {

        ReviewResponse response = reviewService.getReview(reviewId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{reviewId}/restore")
    @Operation(
            summary = "Restore deleted review",
            description = "Restore a soft-deleted review"
    )
    public ResponseEntity<ApiResponse<ReviewResponse>> restoreReview(
            @PathVariable @Positive Long reviewId,
            @RequestParam(required = false) Long userId) {

        ReviewResponse response = reviewService.restoreReview(reviewId, userId);
        return ResponseEntity.ok(ApiResponse.success("Review restored successfully", response));
    }

    // ==================== Query Operations ====================

    @GetMapping("/product/{productId}")
    @Operation(
            summary = "Get product reviews",
            description = "Get paginated list of approved reviews for a specific product"
    )
    public ResponseEntity<ApiResponse<PaginatedResponse<ReviewResponse>>> getProductReviews(
            @PathVariable @Positive Long productId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String direction) {

        Sort.Direction sortDirection = Sort.Direction.fromString(direction);
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));

        Page<ReviewResponse> reviews = reviewService.getProductReviews(productId, pageable);
        return ResponseEntity.ok(ApiResponse.success(PaginatedResponse.from(reviews)));
    }

    @PostMapping("/product/{productId}/filter")
    @Operation(
            summary = "Get filtered product reviews",
            description = "Get reviews for a product with advanced filtering options"
    )
    public ResponseEntity<ApiResponse<PaginatedResponse<ReviewResponse>>> getFilteredReviews(
            @PathVariable @Positive Long productId,
            @Valid @RequestBody ReviewFilterRequest filters,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String direction) {

        Sort.Direction sortDirection = "desc".equalsIgnoreCase(direction)
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));

        Page<ReviewResponse> reviews = reviewService.getProductReviewsWithFilters(productId, filters, pageable);
        return ResponseEntity.ok(ApiResponse.success(PaginatedResponse.from(reviews)));
    }

    @GetMapping("/product/{productId}/verified")
    @Operation(
            summary = "Get verified purchase reviews",
            description = "Get only reviews from verified purchases for a product"
    )
    public ResponseEntity<ApiResponse<PaginatedResponse<ReviewResponse>>> getVerifiedReviews(
            @PathVariable @Positive Long productId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ReviewResponse> reviews = reviewService.getVerifiedReviews(productId, pageable);
        return ResponseEntity.ok(ApiResponse.success(PaginatedResponse.from(reviews)));
    }

    @GetMapping("/product/{productId}/rating/{rating}")
    @Operation(
            summary = "Get reviews by rating",
            description = "Get all reviews for a product with a specific star rating (1-5)"
    )
    public ResponseEntity<ApiResponse<PaginatedResponse<ReviewResponse>>> getReviewsByRating(
            @PathVariable @Positive Long productId,
            @PathVariable @Min(1) @Max(5) Integer rating,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ReviewResponse> reviews = reviewService.getReviewsByRating(productId, rating, pageable);
        return ResponseEntity.ok(ApiResponse.success(PaginatedResponse.from(reviews)));
    }

    @GetMapping("/product/{productId}/helpful")
    @Operation(
            summary = "Get most helpful reviews",
            description = "Get the most helpful reviews for a product based on user votes"
    )
    public ResponseEntity<ApiResponse<List<ReviewResponse>>> getMostHelpfulReviews(
            @PathVariable @Positive Long productId,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int limit) {

        List<ReviewResponse> reviews = reviewService.getMostHelpfulReviews(productId, limit);
        return ResponseEntity.ok(ApiResponse.success(reviews));
    }

    @GetMapping("/product/{productId}/recent")
    @Operation(
            summary = "Get recent reviews",
            description = "Get the most recent reviews for a product"
    )
    public ResponseEntity<ApiResponse<List<ReviewResponse>>> getRecentReviews(
            @PathVariable @Positive Long productId,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int limit) {

        List<ReviewResponse> reviews = reviewService.getRecentReviews(productId, limit);
        return ResponseEntity.ok(ApiResponse.success(reviews));
    }

    @GetMapping("/product/{productId}/with-images")
    @Operation(
            summary = "Get reviews with images",
            description = "Get only reviews that include product images"
    )
    public ResponseEntity<ApiResponse<PaginatedResponse<ReviewResponse>>> getReviewsWithImages(
            @PathVariable @Positive Long productId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ReviewResponse> reviews = reviewService.getReviewsWithImages(productId, pageable);
        return ResponseEntity.ok(ApiResponse.success(PaginatedResponse.from(reviews)));
    }

    @GetMapping("/user/{userId}")
    @Operation(
            summary = "Get user reviews",
            description = "Get all reviews written by a specific user"
    )
    public ResponseEntity<ApiResponse<PaginatedResponse<ReviewResponse>>> getUserReviews(
            @PathVariable @Positive Long userId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ReviewResponse> reviews = reviewService.getUserReviews(userId, pageable);
        return ResponseEntity.ok(ApiResponse.success(PaginatedResponse.from(reviews)));
    }

    @GetMapping("/my-reviews")
    @Operation(
            summary = "Get my reviews",
            description = "Get all reviews written by the authenticated user"
    )
    public ResponseEntity<ApiResponse<PaginatedResponse<ReviewResponse>>> getMyReviews(
            @RequestParam(required = false) Long userId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ReviewResponse> reviews = reviewService.getUserReviews(userId, pageable);
        return ResponseEntity.ok(ApiResponse.success(PaginatedResponse.from(reviews)));
    }

    @GetMapping("/product/{productId}/search")
    @Operation(
            summary = "Search reviews",
            description = "Search reviews by keyword in title and comment"
    )
    public ResponseEntity<ApiResponse<PaginatedResponse<ReviewResponse>>> searchReviews(
            @PathVariable @Positive Long productId,
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ReviewResponse> reviews = reviewService.searchReviews(productId, keyword, pageable);
        return ResponseEntity.ok(ApiResponse.success(PaginatedResponse.from(reviews)));
    }

    // ==================== Statistics & Analytics ====================

    @GetMapping("/product/{productId}/stats")
    @Operation(
            summary = "Get product rating statistics",
            description = "Get comprehensive statistics including average rating, total reviews, rating distribution, and common pros/cons"
    )
    public ResponseEntity<ApiResponse<ReviewSummaryResponse>> getProductStats(
            @PathVariable @Positive Long productId) {

        ReviewSummaryResponse stats = reviewService.getProductRatingStats(productId);
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @GetMapping("/user/{userId}/stats")
    @Operation(
            summary = "Get user review statistics",
            description = "Get statistics about a user's review activity"
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUserStats(
            @PathVariable @Positive Long userId) {

        Map<String, Object> stats = reviewService.getUserReviewStats(userId);
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @GetMapping("/product/{productId}/distribution")
    @Operation(
            summary = "Get rating distribution",
            description = "Get detailed rating distribution with counts and percentages"
    )
    public ResponseEntity<ApiResponse<Map<Integer, Map<String, Object>>>> getRatingDistribution(
            @PathVariable @Positive Long productId) {

        Map<Integer, Map<String, Object>> distribution = reviewService.getRatingDistribution(productId);
        return ResponseEntity.ok(ApiResponse.success(distribution));
    }

    @GetMapping("/product/{productId}/trends")
    @Operation(
            summary = "Get review trends",
            description = "Get review trends over time (monthly)"
    )
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getReviewTrends(
            @PathVariable @Positive Long productId,
            @RequestParam(defaultValue = "12") @Min(1) @Max(36) int months) {

        List<Map<String, Object>> trends = reviewService.getReviewTrends(productId, months);
        return ResponseEntity.ok(ApiResponse.success(trends));
    }

    @GetMapping("/product/{productId}/pros")
    @Operation(
            summary = "Get most common pros",
            description = "Get the most frequently mentioned positive aspects"
    )
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getMostCommonPros(
            @PathVariable @Positive Long productId,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int limit) {

        List<Map<String, Object>> pros = reviewService.getMostCommonPros(productId, limit);
        return ResponseEntity.ok(ApiResponse.success(pros));
    }

    @GetMapping("/product/{productId}/cons")
    @Operation(
            summary = "Get most common cons",
            description = "Get the most frequently mentioned negative aspects"
    )
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getMostCommonCons(
            @PathVariable @Positive Long productId,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int limit) {

        List<Map<String, Object>> cons = reviewService.getMostCommonCons(productId, limit);
        return ResponseEntity.ok(ApiResponse.success(cons));
    }

    @GetMapping("/top-rated-products")
    @Operation(
            summary = "Get top rated products",
            description = "Get products with the highest average ratings"
    )
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getTopRatedProducts(
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int limit) {

        List<Map<String, Object>> products = reviewService.getTopRatedProducts(limit);
        return ResponseEntity.ok(ApiResponse.success(products));
    }

    @GetMapping("/most-reviewed-products")
    @Operation(
            summary = "Get most reviewed products",
            description = "Get products with the most reviews"
    )
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getMostReviewedProducts(
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int limit) {

        List<Map<String, Object>> products = reviewService.getMostReviewedProducts(limit);
        return ResponseEntity.ok(ApiResponse.success(products));
    }

    // ==================== Voting Operations ====================

    @PostMapping("/{reviewId}/helpful")
    @Operation(
            summary = "Mark review as helpful",
            description = "Increment the helpful vote count for a review"
    )
    public ResponseEntity<ApiResponse<Void>> markHelpful(
            @PathVariable @Positive Long reviewId) {

        reviewService.markHelpful(reviewId);
        return ResponseEntity.ok(ApiResponse.success("Review marked as helpful", null));
    }

    @PostMapping("/{reviewId}/not-helpful")
    @Operation(
            summary = "Mark review as not helpful",
            description = "Increment the not helpful vote count for a review"
    )
    public ResponseEntity<ApiResponse<Void>> markNotHelpful(
            @PathVariable @Positive Long reviewId) {

        reviewService.markNotHelpful(reviewId);
        return ResponseEntity.ok(ApiResponse.success("Review marked as not helpful", null));
    }

    // ==================== Admin Operations ====================

    @PostMapping("/{reviewId}/approve")
    @Operation(
            summary = "Approve review (Admin)",
            description = "Approve a pending review for public display"
    )
    public ResponseEntity<ApiResponse<ReviewResponse>> approveReview(
            @PathVariable @Positive Long reviewId) {

        ReviewResponse response = reviewService.approveReview(reviewId);
        return ResponseEntity.ok(ApiResponse.success("Review approved successfully", response));
    }

    @PostMapping("/{reviewId}/reject")
    @Operation(
            summary = "Reject review (Admin)",
            description = "Reject a review with a reason"
    )
    public ResponseEntity<ApiResponse<ReviewResponse>> rejectReview(
            @PathVariable @Positive Long reviewId,
            @RequestParam String reason) {

        ReviewResponse response = reviewService.rejectReview(reviewId, reason);
        return ResponseEntity.ok(ApiResponse.success("Review rejected successfully", response));
    }

    @PostMapping("/{reviewId}/admin-response")
    @Operation(
            summary = "Add admin response (Admin)",
            description = "Add an official admin response to a review"
    )
    public ResponseEntity<ApiResponse<ReviewResponse>> addAdminResponse(
            @PathVariable @Positive Long reviewId,
            @Valid @RequestBody AdminResponseRequest request,
            @RequestParam(required = false) Long adminId) {

        ReviewResponse response = reviewService.addAdminResponse(reviewId, request, adminId);
        return ResponseEntity.ok(ApiResponse.success("Admin response added successfully", response));
    }

    @DeleteMapping("/{reviewId}/admin-response")
    @Operation(
            summary = "Remove admin response (Admin)",
            description = "Remove admin response from a review"
    )
    public ResponseEntity<ApiResponse<ReviewResponse>> removeAdminResponse(
            @PathVariable @Positive Long reviewId) {

        ReviewResponse response = reviewService.removeAdminResponse(reviewId);
        return ResponseEntity.ok(ApiResponse.success("Admin response removed successfully", response));
    }

    @GetMapping("/pending")
    @Operation(
            summary = "Get pending reviews (Admin)",
            description = "Get all reviews awaiting moderation"
    )
    public ResponseEntity<ApiResponse<PaginatedResponse<ReviewResponse>>> getPendingReviews(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "createdAt"));
        Page<ReviewResponse> reviews = reviewService.getPendingReviews(pageable);
        return ResponseEntity.ok(ApiResponse.success(PaginatedResponse.from(reviews)));
    }

    @GetMapping("/flagged")
    @Operation(
            summary = "Get flagged reviews (Admin)",
            description = "Get reviews that have been flagged for review"
    )
    public ResponseEntity<ApiResponse<PaginatedResponse<ReviewResponse>>> getFlaggedReviews(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "notHelpfulCount"));
        Page<ReviewResponse> reviews = reviewService.getFlaggedReviews(pageable);
        return ResponseEntity.ok(ApiResponse.success(PaginatedResponse.from(reviews)));
    }

    @PostMapping("/bulk/approve")
    @Operation(
            summary = "Bulk approve reviews (Admin)",
            description = "Approve multiple reviews at once"
    )
    public ResponseEntity<ApiResponse<Map<String, Integer>>> bulkApproveReviews(
            @RequestBody List<@Positive Long> reviewIds) {

        int count = reviewService.bulkApproveReviews(reviewIds);
        return ResponseEntity.ok(ApiResponse.success(
                count + " reviews approved successfully",
                Map.of("approvedCount", count)
        ));
    }

    @PostMapping("/bulk/reject")
    @Operation(
            summary = "Bulk reject reviews (Admin)",
            description = "Reject multiple reviews at once with a reason"
    )
    public ResponseEntity<ApiResponse<Map<String, Integer>>> bulkRejectReviews(
            @RequestBody List<@Positive Long> reviewIds,
            @RequestParam String reason) {

        int count = reviewService.bulkRejectReviews(reviewIds, reason);
        return ResponseEntity.ok(ApiResponse.success(
                count + " reviews rejected successfully",
                Map.of("rejectedCount", count)
        ));
    }

    // ==================== Utility Operations ====================

    @GetMapping("/can-review/{productId}")
    @Operation(
            summary = "Check if user can review",
            description = "Check if the authenticated user can review a specific product"
    )
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> canUserReview(
            @PathVariable @Positive Long productId,
            @RequestParam(required = false) Long userId) {

        boolean canReview = reviewService.canUserReviewProduct(productId, userId);
        boolean hasPurchased = reviewService.hasUserPurchasedProduct(productId, userId);

        Map<String, Boolean> result = Map.of(
                "canReview", canReview,
                "hasPurchased", hasPurchased
        );

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/product/{productId}/update-verification")
    @Operation(
            summary = "Update verification status (Admin)",
            description = "Update verification status of reviews based on order data"
    )
    public ResponseEntity<ApiResponse<Map<String, Integer>>> updateVerificationStatus(
            @PathVariable @Positive Long productId) {

        int count = reviewService.updateVerificationStatusFromOrders(productId);
        return ResponseEntity.ok(ApiResponse.success(
                count + " reviews updated",
                Map.of("updatedCount", count)
        ));
    }
}

