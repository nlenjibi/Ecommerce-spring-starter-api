package com.smart_ecomernce_api.smart_ecomernce_api.graphql.resolver;

import com.smart_ecomernce_api.smart_ecomernce_api.common.response.PaginatedResponse;
import com.smart_ecomernce_api.smart_ecomernce_api.graphql.dto.ReviewResponseDto;
import com.smart_ecomernce_api.smart_ecomernce_api.graphql.input.PageInput;
import com.smart_ecomernce_api.smart_ecomernce_api.graphql.input.SortDirection;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.review.dto.ReviewCreateRequest;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.review.dto.ReviewResponse;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.review.dto.ReviewSummaryResponse;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.review.dto.ReviewUpdateRequest;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.review.entity.ProductRatingStats;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.review.service.ReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.ContextValue;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
@Slf4j
class ReviewResolver {

    private final ReviewService reviewService;

    @QueryMapping
    public ReviewResponseDto productReviews(
            @Argument Long productId,
            @Argument PageInput pagination) {
        log.info("GraphQL Query: productReviews(productId: {})", productId);
        Pageable pageable = createPageable(pagination);
        Page<ReviewResponse> productReviews = reviewService.getProductReviews(productId, pageable);
        return ReviewResponseDto.builder()
                .content(productReviews.getContent())
                .pageInfo(PaginatedResponse.from(productReviews))
                .build();
    }

    @QueryMapping
    public ReviewSummaryResponse productRatingStats(@Argument Long productId) {
        log.info("GraphQL Query: productRatingStats(productId: {})", productId);
        return reviewService.getProductRatingStats(productId);
    }

    @MutationMapping
    public ReviewResponse createReview(@Argument ReviewCreateRequest input, @ContextValue Long userId) {
        log.info("GraphQL Mutation: createReview for user {}", userId);
        return reviewService.createReview(input, userId);
    }

    @MutationMapping
    public ReviewResponse updateReview(
            @Argument Long id,
            @Argument ReviewUpdateRequest input,
            @ContextValue Long userId) {
        log.info("GraphQL Mutation: updateReview(id: {})", id);
        return reviewService.updateReview(id, input, userId);
    }

    @MutationMapping
    public Boolean deleteReview(@Argument Long id, @ContextValue Long userId) {
        log.info("GraphQL Mutation: deleteReview(id: {})", id);
        reviewService.deleteReview(id, userId);
        return true;
    }

    @MutationMapping
    public ReviewResponse markReviewHelpful(@Argument Long id) {
        log.info("GraphQL Mutation: markReviewHelpful(id: {})", id);
        reviewService.markHelpful(id);
        // Return updated review
        return null; // You'd need to fetch and return the updated review
    }

    private Pageable createPageable(PageInput input) {
        if (input == null) {
            return PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
        }
        Sort sort = input.getDirection() == SortDirection.DESC
                ? Sort.by(input.getSortBy()).descending()
                : Sort.by(input.getSortBy()).ascending();
        return PageRequest.of(input.getPage(), input.getSize(), sort);
    }
}
