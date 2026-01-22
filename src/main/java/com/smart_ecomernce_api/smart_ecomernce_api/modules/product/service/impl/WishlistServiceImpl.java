package com.smart_ecomernce_api.smart_ecomernce_api.modules.product.service.impl;


import com.smart_ecomernce_api.Smart_ecommerce_api.exception.DuplicateResourceException;
import com.smart_ecomernce_api.Smart_ecommerce_api.exception.ResourceNotFoundException;
import com.smart_ecomernce_api.Smart_ecommerce_api.modules.product.dto.AddToWishlistRequest;
import com.smart_ecomernce_api.Smart_ecommerce_api.modules.product.dto.UpdateWishlistItemRequest;
import com.smart_ecomernce_api.Smart_ecommerce_api.modules.product.dto.WishlistItemDto;
import com.smart_ecomernce_api.Smart_ecommerce_api.modules.product.dto.WishlistSummaryDto;
import com.smart_ecomernce_api.Smart_ecommerce_api.modules.product.entity.Product;
import com.smart_ecomernce_api.Smart_ecommerce_api.modules.product.entity.WishlistItem;
import com.smart_ecomernce_api.Smart_ecommerce_api.modules.product.repository.ProductRepository;
import com.smart_ecomernce_api.Smart_ecommerce_api.modules.product.service.WishlistService;
import com.smart_ecomernce_api.Smart_ecommerce_api.modules.user.entity.User;
import com.smart_ecomernce_api.Smart_ecommerce_api.modules.user.mapper.WishlistMapper;
import com.smart_ecomernce_api.Smart_ecommerce_api.modules.user.repository.UserRepository;
import com.smart_ecomernce_api.Smart_ecommerce_api.modules.user.repository.WishlistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class WishlistServiceImpl implements WishlistService {

    private final WishlistRepository wishlistRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final WishlistMapper wishlistMapper;

    @Override
    @Transactional
    @CacheEvict(value = "wishlists", key = "#userId", allEntries = true)
    public WishlistItemDto addToWishlist(Long userId, AddToWishlistRequest request) {
        log.info("Adding product {} to wishlist for user {}", request.getProductId(), userId);

        // Validate user exists
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        // Validate product exists
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found with id: " + request.getProductId()));

        // Check if already in wishlist
        if (wishlistRepository.existsByUserIdAndProductId(userId, request.getProductId())) {
            throw new DuplicateResourceException(
                    "Product already in wishlist for this user");
        }

        // Create wishlist item
        WishlistItem wishlistItem = WishlistItem.builder()
                .user(user)
                .product(product)
                .notes(request.getNotes())
                .priority(request.getPriority())
                .desiredQuantity(request.getDesiredQuantity() != null ? request.getDesiredQuantity() : 1)
                .priceWhenAdded(product.getEffectivePrice())
                .targetPrice(request.getTargetPrice())
                .notifyOnPriceDrop(request.getNotifyOnPriceDrop() != null ? request.getNotifyOnPriceDrop() : false)
                .notifyOnStock(request.getNotifyOnStock() != null ? request.getNotifyOnStock() : false)
                .isPublic(request.getIsPublic() != null ? request.getIsPublic() : false)
                .purchased(false)
                .build();

        WishlistItem saved = wishlistRepository.save(wishlistItem);
        log.info("Product added to wishlist: wishlistItemId={}", saved.getId());

        return wishlistMapper.toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "wishlists", key = "#userId")
    public List<WishlistItemDto> getUserWishlist(Long userId) {
        // Verify user exists
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }

        return wishlistRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(wishlistMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "wishlists", key = "#userId + '_' + #pageable.pageNumber + '_' + #pageable.pageSize")
    public Page<WishlistItemDto> getUserWishlistPaginated(Long userId, Pageable pageable) {
        // Verify user exists
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }

        List<WishlistItem> allItems = wishlistRepository.findByUserIdOrderByCreatedAtDesc(userId);

        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), allItems.size());

        List<WishlistItemDto> pageContent = allItems.subList(start, end).stream()
                .map(wishlistMapper::toDto)
                .collect(Collectors.toList());

        return new PageImpl<>(pageContent, pageable, allItems.size());
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "wishlists", key = "#userId + '_summary'")
    public WishlistSummaryDto getWishlistSummary(Long userId) {
        List<WishlistItem> items = wishlistRepository.findByUserIdOrderByCreatedAtDesc(userId);

        long inStockCount = items.stream()
                .filter(item -> item.getProduct().isInStock())
                .count();

        long outOfStockCount = items.stream()
                .filter(item -> item.getProduct().isOutOfStock())
                .count();

        long priceDropsCount = items.stream()
                .filter(WishlistItem::isPriceDropped)
                .count();

        long purchasedCount = items.stream()
                .filter(WishlistItem::getPurchased)
                .count();

        BigDecimal totalValue = items.stream()
                .map(item -> item.getProduct().getEffectivePrice()
                        .multiply(BigDecimal.valueOf(item.getDesiredQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalSavings = items.stream()
                .filter(WishlistItem::isPriceDropped)
                .map(WishlistItem::getPriceDifference)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return WishlistSummaryDto.builder()
                .userId(userId)
                .totalItems(items.size())
                .inStockItems((int) inStockCount)
                .outOfStockItems((int) outOfStockCount)
                .itemsWithPriceDrops((int) priceDropsCount)
                .purchasedItems((int) purchasedCount)
                .totalValue(totalValue)
                .totalSavings(totalSavings)
                .items(items.stream().map(wishlistMapper::toDto).collect(Collectors.toList()))
                .build();
    }

    @Override
    @Transactional
    @CacheEvict(value = "wishlists", key = "#userId", allEntries = true)
    public void removeFromWishlist(Long userId, Long productId) {
        log.info("Removing product {} from wishlist for user {}", productId, userId);

        WishlistItem item = wishlistRepository.findByUserIdAndProductId(userId, productId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found in wishlist"));

        wishlistRepository.delete(item);
        log.info("Product removed from wishlist: wishlistItemId={}", item.getId());
    }

    @Override
    @Transactional
    @CacheEvict(value = "wishlists", key = "#userId", allEntries = true)
    public WishlistItemDto updateWishlistItem(Long userId, Long productId, UpdateWishlistItemRequest request) {
        log.info("Updating wishlist item for user {} and product {}", userId, productId);

        WishlistItem item = wishlistRepository.findByUserIdAndProductId(userId, productId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found in wishlist"));

        // Update fields if provided
        if (request.getNotes() != null) {
            item.setNotes(request.getNotes());
        }
        if (request.getPriority() != null) {
            item.setPriority(request.getPriority());
        }
        if (request.getDesiredQuantity() != null) {
            item.setDesiredQuantity(request.getDesiredQuantity());
        }
        if (request.getTargetPrice() != null) {
            item.setTargetPrice(request.getTargetPrice());
        }
        if (request.getNotifyOnPriceDrop() != null) {
            item.setNotifyOnPriceDrop(request.getNotifyOnPriceDrop());
        }
        if (request.getNotifyOnStock() != null) {
            item.setNotifyOnStock(request.getNotifyOnStock());
        }
        if (request.getIsPublic() != null) {
            item.setIsPublic(request.getIsPublic());
        }

        WishlistItem updated = wishlistRepository.save(item);
        log.info("Wishlist item updated: wishlistItemId={}", updated.getId());

        return wishlistMapper.toDto(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isInWishlist(Long userId, Long productId) {
        return wishlistRepository.existsByUserIdAndProductId(userId, productId);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "wishlists", key = "#userId + '_pricedrops'")
    public List<WishlistItemDto> getItemsWithPriceDrops(Long userId) {
        return wishlistRepository.findItemsWithPriceDrops(userId).stream()
                .map(wishlistMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    @CacheEvict(value = "wishlists", key = "#userId", allEntries = true)
    public WishlistItemDto markAsPurchased(Long userId, Long productId) {
        log.info("Marking wishlist item as purchased for user {} and product {}", userId, productId);

        WishlistItem item = wishlistRepository.findByUserIdAndProductId(userId, productId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found in wishlist"));

        item.markAsPurchased();
        WishlistItem updated = wishlistRepository.save(item);

        log.info("Wishlist item marked as purchased: wishlistItemId={}", updated.getId());
        return wishlistMapper.toDto(updated);
    }

    @Override
    @Transactional
    @CacheEvict(value = "wishlists", key = "#userId", allEntries = true)
    public void clearWishlist(Long userId) {
        log.info("Clearing wishlist for user {}", userId);
        wishlistRepository.deleteByUserId(userId);
        log.info("Wishlist cleared for user {}", userId);
    }

    @Override
    @Transactional
    @CacheEvict(value = "wishlists", key = "#userId", allEntries = true)
    public void moveToCart(Long userId, Long productId) {
        // This would integrate with cart service
        // For now, just mark as purchased and remove
        log.info("Moving product {} to cart for user {}", productId, userId);

        WishlistItem item = wishlistRepository.findByUserIdAndProductId(userId, productId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found in wishlist"));

        // TODO: Add to cart logic here
        // cartService.addToCart(userId, productId, item.getDesiredQuantity());

        wishlistRepository.delete(item);
        log.info("Product moved to cart and removed from wishlist");
    }
}

