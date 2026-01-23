package com.smart_ecomernce_api.smart_ecomernce_api.graphql.resolver;

import com.smart_ecomernce_api.smart_ecomernce_api.graphql.input.*;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.product.dto.AddToWishlistRequest;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.product.dto.UpdateWishlistItemRequest;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.product.dto.WishlistItemDto;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.product.dto.WishlistSummaryDto;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.product.service.WishlistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.ContextValue;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;
@Controller
@RequiredArgsConstructor
@Slf4j
class WishlistResolver {

    private final WishlistService wishlistService;

    @QueryMapping
    @Cacheable(value = "wishlists", key = "#userId")
    public List<WishlistItemDto> myWishlist(@ContextValue Long userId) {
        log.info("GraphQL Query: myWishlist for user {}", userId);
        return wishlistService.getUserWishlist(userId);
    }

    @QueryMapping
    @Cacheable(value = "wishlists", key = "#userId + '_' + (#pagination==null?0:#pagination.page) + '_' + (#pagination==null?20:#pagination.size)")
    public Page<WishlistItemDto> myWishlistPaginated(
            @Argument PageInput pagination,
            @ContextValue Long userId) {
        log.info("GraphQL Query: myWishlistPaginated for user {}", userId);
        Pageable pageable = createPageable(pagination);
        return wishlistService.getUserWishlistPaginated(userId, pageable);
    }

    @QueryMapping
    @Cacheable(value = "wishlists", key = "#userId + '_summary'")
    public WishlistSummaryDto wishlistSummary(@ContextValue Long userId) {
        log.info("GraphQL Query: wishlistSummary for user {}", userId);
        return wishlistService.getWishlistSummary(userId);
    }

    @QueryMapping
    @Cacheable(value = "wishlists", key = "#userId + '_contains_' + #productId")
    public Boolean isInWishlist(@Argument Long productId, @ContextValue Long userId) {
        log.info("GraphQL Query: isInWishlist(productId: {}) for user {}", productId, userId);
        return wishlistService.isInWishlist(userId, productId);
    }

    @QueryMapping
    @Cacheable(value = "wishlists", key = "#userId + '_pricedrops'")
    public List<WishlistItemDto> wishlistItemsWithPriceDrops(@ContextValue Long userId) {
        log.info("GraphQL Query: wishlistItemsWithPriceDrops for user {}", userId);
        return wishlistService.getItemsWithPriceDrops(userId);
    }

    @MutationMapping
    @CacheEvict(value = "wishlists", key = "#userId", allEntries = true)
    public WishlistItemDto addToWishlist(
            @Argument AddToWishlistRequest input,
            @ContextValue Long userId) {
        log.info("GraphQL Mutation: addToWishlist for user {}", userId);
        return wishlistService.addToWishlist(userId, input);
    }

    @MutationMapping
    @CacheEvict(value = "wishlists", key = "#userId", allEntries = true)
    public WishlistItemDto updateWishlistItem(
            @Argument Long productId,
            @Argument UpdateWishlistItemRequest input,
            @ContextValue Long userId) {
        log.info("GraphQL Mutation: updateWishlistItem(productId: {})", productId);
        return wishlistService.updateWishlistItem(userId, productId, input);
    }

    @MutationMapping
    @CacheEvict(value = "wishlists", key = "#userId", allEntries = true)
    public Boolean removeFromWishlist(@Argument Long productId, @ContextValue Long userId) {
        log.info("GraphQL Mutation: removeFromWishlist(productId: {})", productId);
        wishlistService.removeFromWishlist(userId, productId);
        return true;
    }

    @MutationMapping
    @CacheEvict(value = "wishlists", key = "#userId", allEntries = true)
    public Boolean clearWishlist(@ContextValue Long userId) {
        log.info("GraphQL Mutation: clearWishlist for user {}", userId);
        wishlistService.clearWishlist(userId);
        return true;
    }

    @MutationMapping
    @CacheEvict(value = "wishlists", key = "#userId", allEntries = true)
    public WishlistItemDto markWishlistItemPurchased(
            @Argument Long productId,
            @ContextValue Long userId) {
        log.info("GraphQL Mutation: markWishlistItemPurchased(productId: {})", productId);
        return wishlistService.markAsPurchased(userId, productId);
    }

    private Pageable createPageable(PageInput input) {
        if (input == null) {
            return PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
        }
        Sort sort = input.getDirection() == SortDirection.DESC
                ? Sort.by(input.getSortBy()).descending()
                : Sort.by(input.getSortBy()).ascending();
        return PageRequest.of(input.getPage(), input.getSize(), sort);
    }
}
