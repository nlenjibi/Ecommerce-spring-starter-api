package com.smart_ecomernce_api.smart_ecomernce_api.modules.product.controller;

import com.smart_ecomernce_api.Smart_ecommerce_api.common.response.ApiResponse;
import com.smart_ecomernce_api.Smart_ecommerce_api.modules.product.dto.AddToWishlistRequest;
import com.smart_ecomernce_api.Smart_ecommerce_api.modules.product.dto.UpdateWishlistItemRequest;
import com.smart_ecomernce_api.Smart_ecommerce_api.modules.product.dto.WishlistItemDto;
import com.smart_ecomernce_api.Smart_ecommerce_api.modules.product.dto.WishlistSummaryDto;
import com.smart_ecomernce_api.Smart_ecommerce_api.modules.product.service.WishlistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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

import java.util.List;

@RestController
@RequestMapping("api/v1/wishlist")
@RequiredArgsConstructor
@Tag(name = "Wishlist Management", description = "User wishlist and saved items management")
public class WishlistController {

    private final WishlistService wishlistService;

    @PostMapping
    @Operation(summary = "Add product to wishlist",
            description = "Add a product to the authenticated user's wishlist with optional preferences")
    public ResponseEntity<ApiResponse<WishlistItemDto>> addToWishlist(
            @Parameter(description = "User ID", required = true)
            @RequestParam Long userId,
            @Valid @RequestBody AddToWishlistRequest request) {

        WishlistItemDto item = wishlistService.addToWishlist(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Product added to wishlist successfully", item));
    }

    @GetMapping
    @Operation(summary = "Get user wishlist",
            description = "Retrieve all items in user's wishlist")
    public ResponseEntity<ApiResponse<List<WishlistItemDto>>> getWishlist(
            @Parameter(description = "User ID", required = true)
            @RequestParam Long userId) {

        List<WishlistItemDto> items = wishlistService.getUserWishlist(userId);
        return ResponseEntity.ok(
                ApiResponse.success("Wishlist retrieved successfully", items));
    }

    @GetMapping("/paginated")
    @Operation(summary = "Get user wishlist with pagination",
            description = "Retrieve wishlist items with pagination and sorting")
    public ResponseEntity<ApiResponse<Page<WishlistItemDto>>> getWishlistPaginated(
            @Parameter(description = "User ID", required = true)
            @RequestParam Long userId,
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort by field")
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Sort direction")
            @RequestParam(defaultValue = "DESC") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("ASC")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<WishlistItemDto> items = wishlistService.getUserWishlistPaginated(userId, pageable);
        return ResponseEntity.ok(
                ApiResponse.success("Wishlist retrieved successfully", items));
    }

    @GetMapping("/summary")
    @Operation(summary = "Get wishlist summary",
            description = "Get statistics and summary of user's wishlist")
    public ResponseEntity<ApiResponse<WishlistSummaryDto>> getWishlistSummary(
            @Parameter(description = "User ID", required = true)
            @RequestParam Long userId) {

        WishlistSummaryDto summary = wishlistService.getWishlistSummary(userId);
        return ResponseEntity.ok(
                ApiResponse.success("Wishlist summary retrieved successfully", summary));
    }

    @GetMapping("/check/{productId}")
    @Operation(summary = "Check if product is in wishlist",
            description = "Check whether a specific product is in user's wishlist")
    public ResponseEntity<ApiResponse<Boolean>> checkInWishlist(
            @Parameter(description = "User ID", required = true)
            @RequestParam Long userId,
            @Parameter(description = "Product ID", required = true)
            @PathVariable Long productId) {

        boolean isInWishlist = wishlistService.isInWishlist(userId, productId);
        return ResponseEntity.ok(
                ApiResponse.success("Wishlist check completed", isInWishlist));
    }

    @GetMapping("/price-drops")
    @Operation(summary = "Get items with price drops",
            description = "Retrieve all wishlist items that currently have price drops")
    public ResponseEntity<ApiResponse<List<WishlistItemDto>>> getItemsWithPriceDrops(
            @Parameter(description = "User ID", required = true)
            @RequestParam Long userId) {

        List<WishlistItemDto> items = wishlistService.getItemsWithPriceDrops(userId);
        return ResponseEntity.ok(
                ApiResponse.success("Items with price drops retrieved successfully", items));
    }

    @PutMapping("/{productId}")
    @Operation(summary = "Update wishlist item",
            description = "Update preferences for a wishlist item (notes, priority, notifications, etc.)")
    public ResponseEntity<ApiResponse<WishlistItemDto>> updateWishlistItem(
            @Parameter(description = "User ID", required = true)
            @RequestParam Long userId,
            @Parameter(description = "Product ID", required = true)
            @PathVariable Long productId,
            @Valid @RequestBody UpdateWishlistItemRequest request) {

        WishlistItemDto updated = wishlistService.updateWishlistItem(userId, productId, request);
        return ResponseEntity.ok(
                ApiResponse.success("Wishlist item updated successfully", updated));
    }

    @PatchMapping("/{productId}/purchase")
    @Operation(summary = "Mark item as purchased",
            description = "Mark a wishlist item as purchased without removing it")
    public ResponseEntity<ApiResponse<WishlistItemDto>> markAsPurchased(
            @Parameter(description = "User ID", required = true)
            @RequestParam Long userId,
            @Parameter(description = "Product ID", required = true)
            @PathVariable Long productId) {

        WishlistItemDto item = wishlistService.markAsPurchased(userId, productId);
        return ResponseEntity.ok(
                ApiResponse.success("Item marked as purchased", item));
    }

    @PostMapping("/{productId}/move-to-cart")
    @Operation(summary = "Move item to cart",
            description = "Move a wishlist item to shopping cart and remove from wishlist")
    public ResponseEntity<ApiResponse<Void>> moveToCart(
            @Parameter(description = "User ID", required = true)
            @RequestParam Long userId,
            @Parameter(description = "Product ID", required = true)
            @PathVariable Long productId) {

        wishlistService.moveToCart(userId, productId);
        return ResponseEntity.ok(
                ApiResponse.success("Item moved to cart successfully", null));
    }

    @DeleteMapping("/{productId}")
    @Operation(summary = "Remove from wishlist",
            description = "Remove a specific product from user's wishlist")
    public ResponseEntity<ApiResponse<Void>> removeFromWishlist(
            @Parameter(description = "User ID", required = true)
            @RequestParam Long userId,
            @Parameter(description = "Product ID", required = true)
            @PathVariable Long productId) {

        wishlistService.removeFromWishlist(userId, productId);
        return ResponseEntity.ok(
                ApiResponse.success("Product removed from wishlist successfully", null));
    }

    @DeleteMapping("/clear")
    @Operation(summary = "Clear entire wishlist",
            description = "Remove all items from user's wishlist")
    public ResponseEntity<ApiResponse<Void>> clearWishlist(
            @Parameter(description = "User ID", required = true)
            @RequestParam Long userId) {

        wishlistService.clearWishlist(userId);
        return ResponseEntity.ok(
                ApiResponse.success("Wishlist cleared successfully", null));
    }
}
