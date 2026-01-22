package com.smart_ecomernce_api.smart_ecomernce_api.modules.review.controller;

import com.smart_ecomernce_api.Smart_ecommerce_api.common.response.ApiResponse;
import com.smart_ecomernce_api.Smart_ecommerce_api.common.response.PaginatedResponse;
import com.smart_ecomernce_api.Smart_ecommerce_api.modules.review.dto.ReviewCreateRequest;
import com.smart_ecomernce_api.Smart_ecommerce_api.modules.review.dto.ReviewResponse;
import com.smart_ecomernce_api.Smart_ecommerce_api.modules.review.dto.ReviewUpdateRequest;
import com.smart_ecomernce_api.Smart_ecommerce_api.modules.review.entity.ProductRatingStats;
import com.smart_ecomernce_api.Smart_ecommerce_api.modules.review.service.ReviewService;
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

@RestController
@RequestMapping("v1/reviews")
@RequiredArgsConstructor
@Tag(name = "Product Reviews", description = "APIs for managing product reviews")
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    @Operation(summary = "Create review", description = "Create a new product review")
    public ResponseEntity<ApiResponse<ReviewResponse>> createReview(
            @Valid @RequestBody ReviewCreateRequest request,
            Long userId) {

        ReviewResponse response = reviewService.createReview(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Review created successfully", response));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update review", description = "Update your product review")
    public ResponseEntity<ApiResponse<ReviewResponse>> updateReview(
            @PathVariable Long id,
            @Valid @RequestBody ReviewUpdateRequest request,
            Long userId) {
        ReviewResponse response = reviewService.updateReview(id, request, userId);
        return ResponseEntity.ok(ApiResponse.success("Review updated successfully", response));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete review", description = "Delete your product review")
    public ResponseEntity<ApiResponse<Void>> deleteReview(
            @PathVariable Long id,
            Long userId) {
        reviewService.deleteReview(id, userId);
        return ResponseEntity.ok(ApiResponse.success("Review deleted successfully", null));
    }

    @GetMapping("/product/{productId}")
    @Operation(summary = "Get product reviews", description = "Get all reviews for a product")
    public ResponseEntity<ApiResponse<PaginatedResponse<ReviewResponse>>> getProductReviews(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        Page<ReviewResponse> reviews = reviewService.getProductReviews(productId, pageable);
        return ResponseEntity.ok(ApiResponse.success(PaginatedResponse.from(reviews)));
    }

    @GetMapping("/product/{productId}/stats")
    @Operation(summary = "Get rating statistics", description = "Get rating statistics for a product")
    public ResponseEntity<ApiResponse<ProductRatingStats>> getProductStats(
            @PathVariable Long productId) {
        ProductRatingStats stats = reviewService.getProductRatingStats(productId);
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @PostMapping("/{id}/helpful")
    @Operation(summary = "Mark helpful", description = "Mark review as helpful")
    public ResponseEntity<ApiResponse<Void>> markHelpful(@PathVariable Long id) {
        reviewService.markHelpful(id);
        return ResponseEntity.ok(ApiResponse.success("Marked as helpful", null));
    }

    @PostMapping("/{id}/not-helpful")
    @Operation(summary = "Mark not helpful", description = "Mark review as not helpful")
    public ResponseEntity<ApiResponse<Void>> markNotHelpful(@PathVariable Long id) {
        reviewService.markNotHelpful(id);
        return ResponseEntity.ok(ApiResponse.success("Marked as not helpful", null));
    }


}
