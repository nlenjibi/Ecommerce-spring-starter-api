package com.smart_ecomernce_api.smart_ecomernce_api.modules.product.service.impl;

import com.smart_ecomernce_api.smart_ecomernce_api.exception.DuplicateResourceException;
import com.smart_ecomernce_api.smart_ecomernce_api.exception.ResourceNotFoundException;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.product.dto.*;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.product.entity.Product;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.product.entity.WishlistItem;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.product.entity.WishlistPriority;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.product.mapper.WishlistMapper;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.product.repository.ProductRepository;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.product.repository.WishlistRepository;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.product.service.WishlistService;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.user.entity.User;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class WishlistServiceImpl implements WishlistService {

    private final WishlistRepository wishlistRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final WishlistMapper wishlistMapper;

    // ==================== Basic Operations ====================

    @Override
    @Transactional
    public WishlistItemDto addToWishlist(Long userId, AddToWishlistRequest request) {
        log.info("Adding product {} to wishlist for user {}", request.getProductId(), userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found with id: " + request.getProductId()));

        if (!product.getIsActive()) {
            throw new ResourceNotFoundException("Product is not available");
        }

        if (wishlistRepository.existsByUserIdAndProductId(userId, request.getProductId())) {
            throw new DuplicateResourceException("Product already in wishlist");
        }

        WishlistItem wishlistItem = WishlistItem.builder()
                .user(user)
                .product(product)
                .notes(request.getNotes())
                .priority(request.getPriority() != null ? request.getPriority() : WishlistPriority.MEDIUM)
                .desiredQuantity(request.getDesiredQuantity() != null ? request.getDesiredQuantity() : 1)
                .priceWhenAdded(product.getEffectivePrice())
                .targetPrice(request.getTargetPrice())
                .notifyOnPriceDrop(request.getNotifyOnPriceDrop() != null ? request.getNotifyOnPriceDrop() : false)
                .notifyOnStock(request.getNotifyOnStock() != null ? request.getNotifyOnStock() : false)
                .isPublic(request.getIsPublic() != null ? request.getIsPublic() : false)
                .purchased(false)
                .priceDropCount(0)
                .build();

        WishlistItem saved = wishlistRepository.save(wishlistItem);
        log.info("Product added to wishlist: wishlistItemId={}", saved.getId());

        return wishlistMapper.toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "wishlists", key = "#userId")
    public List<WishlistItemDto> getUserWishlist(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }

        return wishlistRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(wishlistMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "wishlists", key = "#userId + '_page_' + #pageable.pageNumber")
    public Page<WishlistItemDto> getUserWishlistPaginated(Long userId, Pageable pageable) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }

        Page<WishlistItem> itemsPage = wishlistRepository.findByUserId(userId, pageable);
        List<WishlistItemDto> dtos = itemsPage.getContent().stream()
                .map(wishlistMapper::toDto)
                .collect(Collectors.toList());

        return new PageImpl<>(dtos, pageable, itemsPage.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "wishlists", key = "#userId + '_summary'")
    public WishlistSummaryDto getWishlistSummary(Long userId) {
        List<WishlistItem> items = wishlistRepository.findByUserIdOrderByCreatedAtDesc(userId);

        long inStockCount = items.stream()
                .filter(item -> item.getProduct() != null && item.getProduct().isInStock())
                .count();

        long outOfStockCount = items.stream()
                .filter(item -> item.getProduct() != null && item.getProduct().isOutOfStock())
                .count();

        long priceDropsCount = items.stream()
                .filter(WishlistItem::isPriceDropped)
                .count();

        long purchasedCount = items.stream()
                .filter(WishlistItem::getPurchased)
                .count();

        BigDecimal totalValue = items.stream()
                .filter(item -> item.getProduct() != null && item.getProduct().getEffectivePrice() != null)
                .map(item -> item.getProduct().getEffectivePrice()
                        .multiply(BigDecimal.valueOf(item.getDesiredQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalSavings = items.stream()
                .filter(WishlistItem::isPriceDropped)
                .filter(item -> item.getProduct() != null && item.getProduct().getEffectivePrice() != null)
                .map(item -> item.getPriceDifference().multiply(BigDecimal.valueOf(item.getDesiredQuantity())))
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
                .orElseThrow(() -> new ResourceNotFoundException("Product not found in wishlist"));

        wishlistRepository.delete(item);
        log.info("Product removed from wishlist: wishlistItemId={}", item.getId());
    }

    @Override
    @Transactional
    @CacheEvict(value = "wishlists", key = "#userId", allEntries = true)
    public WishlistItemDto updateWishlistItem(Long userId, Long productId, UpdateWishlistItemRequest request) {
        log.info("Updating wishlist item for user {} and product {}", userId, productId);

        WishlistItem item = wishlistRepository.findByUserIdAndProductId(userId, productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found in wishlist"));

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
    @Transactional
    @CacheEvict(value = "wishlists", key = "#userId", allEntries = true)
    public void clearWishlist(Long userId) {
        log.info("Clearing wishlist for user {}", userId);
        wishlistRepository.deleteByUserId(userId);
        log.info("Wishlist cleared for user {}", userId);
    }

    // Part 2 of WishlistServiceImpl - Add these methods to the service class

    // ==================== Price & Stock Tracking ====================

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "wishlists", key = "#userId + '_pricedrops'")
    public List<WishlistItemDto> getItemsWithPriceDrops(Long userId) {
        return wishlistRepository.findItemsWithPriceDrops(userId).stream()
                .map(wishlistMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<WishlistItemDto> getItemsNeedingStockNotification(Long userId) {
        return wishlistRepository.findItemsNeedingStockNotification(userId).stream()
                .map(wishlistMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<WishlistItemDto> getItemsBelowTargetPrice(Long userId) {
        return wishlistRepository.findItemsBelowTargetPrice(userId).stream()
                .map(wishlistMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    @Scheduled(cron = "0 0 */6 * * ?") // Every 6 hours
    public void updateWishlistPrices() {
        log.info("Updating wishlist prices for all users");
        // Fetch all users or all wishlists as needed
        // Example: update for all users
        List<Long> userIds = wishlistRepository.findAllUserIdsWithWishlists(); // You may need to implement this method
        for (Long userId : userIds) {
            List<WishlistItem> items = wishlistRepository.findByUserIdOrderByCreatedAtDesc(userId);
            items.forEach(item -> {
                BigDecimal currentPrice = item.getProduct().getEffectivePrice();
                BigDecimal previousPrice = item.getPriceWhenAdded();
                if (currentPrice.compareTo(previousPrice) < 0) {
                    item.setPriceDropCount(item.getPriceDropCount() + 1);
                    wishlistRepository.save(item);
                }
                item.setLastPriceCheck(LocalDateTime.now());
            });
            log.info("Wishlist prices updated for user {}", userId);
        }
    }

    // ==================== Collections & Organization ====================

    @Override
    @Transactional(readOnly = true)
    public List<WishlistItemDto> getWishlistByCollection(Long userId, String collectionName) {
        return wishlistRepository.findByUserIdAndCollectionName(userId, collectionName).stream()
                .map(wishlistMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getUserCollections(Long userId) {
        return wishlistRepository.findDistinctCollectionsByUserId(userId);
    }

    @Override
    @Transactional
    public void moveItemsToCollection(Long userId, List<Long> productIds, String collectionName) {
        log.info("Moving {} items to collection '{}' for user {}", productIds.size(), collectionName, userId);

        wishlistRepository.bulkUpdateCollection(userId, productIds, collectionName);

        log.info("Items moved to collection successfully");
    }

    @Override
    @Transactional(readOnly = true)
    public List<WishlistItemDto> getWishlistByPriority(Long userId, WishlistPriority priority) {
        return wishlistRepository.findByUserIdAndPriorityOrderByCreatedAtDesc(userId, priority).stream()
                .map(wishlistMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<WishlistItemDto> getWishlistByTags(Long userId, List<String> tags) {
        List<WishlistItem> allItems = new ArrayList<>();

        for (String tag : tags) {
            List<WishlistItem> items = wishlistRepository.findByUserIdAndTagsContaining(userId, tag);
            allItems.addAll(items);
        }

        // Remove duplicates
        return allItems.stream()
                .distinct()
                .map(wishlistMapper::toDto)
                .collect(Collectors.toList());
    }

    // ==================== Purchase & Cart Operations ====================

    @Override
    @Transactional
    public WishlistItemDto markAsPurchased(Long userId, Long productId) {
        log.info("Marking wishlist item as purchased for user {} and product {}", userId, productId);

        WishlistItem item = wishlistRepository.findByUserIdAndProductId(userId, productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found in wishlist"));

        item.markAsPurchased();
        WishlistItem updated = wishlistRepository.save(item);

        log.info("Wishlist item marked as purchased: wishlistItemId={}", updated.getId());
        return wishlistMapper.toDto(updated);
    }

    @Override
    @Transactional
    public void markMultipleAsPurchased(Long userId, List<Long> productIds) {
        log.info("Marking {} items as purchased for user {}", productIds.size(), userId);

        List<WishlistItem> items = wishlistRepository.findByUserIdAndProductIdIn(userId, productIds);

        List<Long> itemIds = items.stream()
                .map(WishlistItem::getId)
                .collect(Collectors.toList());

        wishlistRepository.markMultipleAsPurchased(itemIds);

        log.info("Multiple items marked as purchased");
    }

    @Override
    @Transactional
    public void moveToCart(Long userId, Long productId) {
        log.info("Moving product {} to cart for user {}", productId, userId);

        WishlistItem item = wishlistRepository.findByUserIdAndProductId(userId, productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found in wishlist"));

        // TODO: Add to cart logic here
        // cartService.addToCart(userId, productId, item.getDesiredQuantity());

        wishlistRepository.delete(item);
        log.info("Product moved to cart and removed from wishlist");
    }

    @Override
    @Transactional
    public void moveMultipleToCart(Long userId, List<Long> productIds) {
        log.info("Moving {} products to cart for user {}", productIds.size(), userId);

        List<WishlistItem> items = wishlistRepository.findByUserIdAndProductIdIn(userId, productIds);

        // TODO: Bulk add to cart
        // items.forEach(item -> cartService.addToCart(userId, item.getProduct().getId(), item.getDesiredQuantity()));

        wishlistRepository.deleteByUserIdAndProductIdIn(userId, productIds);

        log.info("Multiple products moved to cart");
    }

    @Override
    @Transactional(readOnly = true)
    public List<WishlistItemDto> getPurchasedItems(Long userId) {
        return wishlistRepository.findPurchasedByUserId(userId).stream()
                .map(wishlistMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<WishlistItemDto> getUnpurchasedItems(Long userId) {
        return wishlistRepository.findUnpurchasedByUserId(userId).stream()
                .map(wishlistMapper::toDto)
                .collect(Collectors.toList());
    }

    // ==================== Sharing & Social Features ====================

    @Override
    @Transactional
    public WishlistShareDto shareWishlist(Long userId, WishlistShareRequest request) {
        log.info("Creating shareable link for user {} wishlist", userId);

        String shareToken = UUID.randomUUID().toString();

        // Update items to public if sharing
        if (request.getProductIds() != null && !request.getProductIds().isEmpty()) {
            wishlistRepository.updatePublicStatusForItems(
                    request.getProductIds().stream()
                            .map(id -> wishlistRepository.findByUserIdAndProductId(userId, id)
                                    .map(WishlistItem::getId)
                                    .orElse(null))
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList()),
                    true
            );
        } else {
            wishlistRepository.updatePublicStatus(userId, true);
        }

        List<WishlistItem> items = request.getProductIds() != null
                ? wishlistRepository.findByUserIdAndProductIdIn(userId, request.getProductIds())
                : wishlistRepository.findPublicItemsByUserId(userId);

        // TODO: Store share token in a shares table with metadata

        return WishlistShareDto.builder()
                .shareToken(shareToken)
                .shareUrl("/api/v1/wishlist/shared/" + shareToken)
                .shareName(request.getShareName())
                .description(request.getDescription())
                .createdAt(LocalDateTime.now())
                .expiresAt(request.getExpiresAt())
                .itemCount(items.size())
                .isActive(true)
                .passwordProtected(request.getPassword() != null)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public WishlistSummaryDto getPublicWishlist(String shareToken) {
        // TODO: Retrieve userId from share token
        // For now, throwing not implemented
        throw new UnsupportedOperationException("Share token lookup not implemented yet");
    }

    @Override
    @Transactional(readOnly = true)
    public List<WishlistItemDto> getPublicWishlistItems(Long userId) {
        return wishlistRepository.findPublicItemsByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(wishlistMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void updateWishlistPrivacy(Long userId, boolean isPublic) {
        log.info("Updating wishlist privacy for user {} to {}", userId, isPublic);
        wishlistRepository.updatePublicStatus(userId, isPublic);
    }
// Part 3 of WishlistServiceImpl - Add these methods to the service class

    // ==================== Bulk Operations ====================

    @Override
    @Transactional
    public List<WishlistItemDto> addMultipleToWishlist(Long userId, List<AddToWishlistRequest> requests) {
        log.info("Adding {} products to wishlist for user {}", requests.size(), userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        List<WishlistItem> items = new ArrayList<>();

        for (AddToWishlistRequest request : requests) {
            try {
                Product product = productRepository.findById(request.getProductId())
                        .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

                if (!product.getIsActive()) {
                    log.warn("Skipping inactive product: {}", request.getProductId());
                    continue;
                }

                if (wishlistRepository.existsByUserIdAndProductId(userId, request.getProductId())) {
                    log.warn("Product {} already in wishlist", request.getProductId());
                    continue;
                }

                WishlistItem item = WishlistItem.builder()
                        .user(user)
                        .product(product)
                        .notes(request.getNotes())
                        .priority(request.getPriority() != null ? request.getPriority() : WishlistPriority.MEDIUM)
                        .desiredQuantity(request.getDesiredQuantity() != null ? request.getDesiredQuantity() : 1)
                        .priceWhenAdded(product.getEffectivePrice())
                        .targetPrice(request.getTargetPrice())
                        .notifyOnPriceDrop(request.getNotifyOnPriceDrop() != null ? request.getNotifyOnPriceDrop() : false)
                        .notifyOnStock(request.getNotifyOnStock() != null ? request.getNotifyOnStock() : false)
                        .isPublic(request.getIsPublic() != null ? request.getIsPublic() : false)
                        .purchased(false)
                        .build();

                items.add(item);
            } catch (Exception e) {
                log.error("Error adding product {} to wishlist", request.getProductId(), e);
            }
        }

        List<WishlistItem> saved = wishlistRepository.saveAll(items);
        log.info("Added {} products to wishlist", saved.size());

        return saved.stream()
                .map(wishlistMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void removeMultipleFromWishlist(Long userId, List<Long> productIds) {
        log.info("Removing {} products from wishlist for user {}", productIds.size(), userId);
        wishlistRepository.deleteByUserIdAndProductIdIn(userId, productIds);
        log.info("Products removed from wishlist");
    }

    @Override
    @Transactional
    public void updateMultipleItems(Long userId, Map<Long, UpdateWishlistItemRequest> updates) {
        log.info("Updating {} wishlist items for user {}", updates.size(), userId);

        updates.forEach((productId, request) -> {
            try {
                updateWishlistItem(userId, productId, request);
            } catch (Exception e) {
                log.error("Error updating wishlist item for product {}", productId, e);
            }
        });
    }

    // ==================== Reminders & Notifications ====================

    @Override
    @Transactional
    public WishlistItemDto setReminder(Long userId, Long productId, WishlistReminderRequest request) {
        log.info("Setting reminder for user {} product {}", userId, productId);

        WishlistItem item = wishlistRepository.findByUserIdAndProductId(userId, productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found in wishlist"));

        item.setReminderEnabled(true);
        item.setReminderDate(request.getReminderDate());
        item.setNotes(item.getNotes() != null
                ? item.getNotes() + "\n[Reminder: " + request.getReminderNote() + "]"
                : "[Reminder: " + request.getReminderNote() + "]");

        WishlistItem updated = wishlistRepository.save(item);
        log.info("Reminder set for wishlist item {}", updated.getId());

        return wishlistMapper.toDto(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WishlistItemDto> getItemsWithDueReminders(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        return wishlistRepository.findItemsWithDueReminders(now).stream()
                .filter(item -> item.getUser().getId().equals(userId))
                .map(wishlistMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void cancelReminder(Long userId, Long productId) {
        log.info("Cancelling reminder for user {} product {}", userId, productId);

        WishlistItem item = wishlistRepository.findByUserIdAndProductId(userId, productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found in wishlist"));

        wishlistRepository.cancelReminder(item.getId());
        log.info("Reminder cancelled");
    }

    // ==================== Analytics & Insights ====================

    @Override
    @Transactional(readOnly = true)
    public WishlistAnalyticsDto getWishlistAnalytics(Long userId) {
        List<WishlistItem> items = wishlistRepository.findByUserIdOrderByCreatedAtDesc(userId);

        LocalDateTime monthAgo = LocalDateTime.now().minusMonths(1);
        long itemsAddedThisMonth = wishlistRepository.countByUserIdAndCreatedAtAfter(userId, monthAgo);

        long purchasedCount = items.stream()
                .filter(WishlistItem::getPurchased)
                .count();

        long priceDropsCount = items.stream()
                .filter(WishlistItem::isPriceDropped)
                .count();

        Double avgPriceDrop = items.stream()
                .filter(WishlistItem::isPriceDropped)
                .mapToDouble(item -> item.getPriceDropPercentage().doubleValue())
                .average()
                .orElse(0.0);

        Double totalSavings = wishlistRepository.calculateTotalSavingsByUserId(userId);

        // Category analytics
        Map<String, List<WishlistItem>> itemsByCategory = items.stream()
                .collect(Collectors.groupingBy(item ->
                        item.getProduct().getCategory() != null
                                ? item.getProduct().getCategory().getName()
                                : "Uncategorized"));

        String mostAddedCategory = itemsByCategory.entrySet().stream()
                .max(Comparator.comparingInt(e -> e.getValue().size()))
                .map(Map.Entry::getKey)
                .orElse("None");

        List<WishlistAnalyticsDto.CategoryAnalytics> categoryBreakdown = itemsByCategory.entrySet().stream()
                .map(entry -> {
                    List<WishlistItem> categoryItems = entry.getValue();
                    Double totalValue = categoryItems.stream()
                            .mapToDouble(item -> item.getProduct().getEffectivePrice().doubleValue())
                            .sum();
                    Double avgPrice = totalValue / categoryItems.size();

                    return WishlistAnalyticsDto.CategoryAnalytics.builder()
                            .categoryName(entry.getKey())
                            .itemCount(categoryItems.size())
                            .totalValue(totalValue)
                            .averagePrice(avgPrice)
                            .build();
                })
                .collect(Collectors.toList());

        return WishlistAnalyticsDto.builder()
                .userId(userId)
                .totalItems(items.size())
                .itemsAddedThisMonth((int) itemsAddedThisMonth)
                .itemsPurchased((int) purchasedCount)
                .itemsWithPriceDrops((int) priceDropsCount)
                .averagePriceDrop(avgPriceDrop)
                .totalSavings(totalSavings)
                .mostAddedCategory(mostAddedCategory)
                .categoryBreakdown(categoryBreakdown)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PriceHistoryDto> getPriceHistory(Long userId, Long productId) {
        // TODO: Implement price history tracking
        // This would require a separate price_history table
        throw new UnsupportedOperationException("Price history tracking not implemented yet");
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductRecommendationDto> getWishlistRecommendations(Long userId) {
        // TODO: Implement ML-based recommendations
        // This would integrate with a recommendation engine
        throw new UnsupportedOperationException("Recommendations not implemented yet");
    }

    // ==================== Import/Export ====================

    @Override
    @Transactional(readOnly = true)
    public byte[] exportWishlistToCsv(Long userId) {
        log.info("Exporting wishlist to CSV for user {}", userId);

        List<WishlistItem> items = wishlistRepository.findByUserIdOrderByCreatedAtDesc(userId);

        StringBuilder csv = new StringBuilder();
        csv.append("Product Name,SKU,Price,Priority,Desired Quantity,Notes,Added Date\n");

        items.forEach(item -> {
            Product product = item.getProduct();
            csv.append(String.format("\"%s\",\"%s\",%.2f,%s,%d,\"%s\",%s\n",
                    product.getName(),
                    product.getSku(),
                    product.getEffectivePrice().doubleValue(),
                    item.getPriority(),
                    item.getDesiredQuantity(),
                    item.getNotes() != null ? item.getNotes().replace("\"", "\"\"") : "",
                    item.getCreatedAt()));
        });

        return csv.toString().getBytes();
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportWishlistToPdf(Long userId) {
        // TODO: Implement PDF export using iText or similar
        throw new UnsupportedOperationException("PDF export not implemented yet");
    }

    @Override
    @Transactional
    @CacheEvict(value = "wishlists", key = "#userId", allEntries = true)
    public void importWishlistFromCsv(Long userId, byte[] csvData) {
        // TODO: Implement CSV import
        throw new UnsupportedOperationException("CSV import not implemented yet");
    }

// Part 4 of WishlistServiceImpl - Final methods

    // ==================== Comparison & Shopping ====================

    @Override
    @Transactional(readOnly = true)
    public WishlistPriceComparisonDto compareWishlistPrices(Long userId) {
        log.info("Comparing wishlist prices for user {}", userId);

        List<WishlistItem> items = wishlistRepository.findByUserIdOrderByCreatedAtDesc(userId);

        double totalOriginalPrice = items.stream()
                .mapToDouble(item -> item.getPriceWhenAdded().doubleValue() * item.getDesiredQuantity())
                .sum();

        double totalCurrentPrice = items.stream()
                .mapToDouble(item -> item.getProduct().getEffectivePrice().doubleValue() * item.getDesiredQuantity())
                .sum();

        double totalSavings = totalOriginalPrice - totalCurrentPrice;
        double averageDiscount = totalOriginalPrice > 0
                ? (totalSavings / totalOriginalPrice) * 100
                : 0.0;

        // Find best deals (highest savings percentage)
        List<WishlistItem> sortedByDiscount = items.stream()
                .filter(WishlistItem::isPriceDropped)
                .sorted((a, b) -> b.getPriceDropPercentage().compareTo(a.getPriceDropPercentage()))
                .collect(Collectors.toList());

        Long bestDealId = sortedByDiscount.isEmpty() ? null : sortedByDiscount.get(0).getProduct().getId();

        List<WishlistPriceComparisonDto.PriceComparisonItem> comparisonItems = items.stream()
                .map(item -> {
                    double originalPrice = item.getPriceWhenAdded().doubleValue() * item.getDesiredQuantity();
                    double currentPrice = item.getProduct().getEffectivePrice().doubleValue() * item.getDesiredQuantity();
                    double savings = originalPrice - currentPrice;
                    double discountPercentage = originalPrice > 0 ? (savings / originalPrice) * 100 : 0.0;

                    return WishlistPriceComparisonDto.PriceComparisonItem.builder()
                            .productId(item.getProduct().getId())
                            .productName(item.getProduct().getName())
                            .originalPrice(originalPrice)
                            .currentPrice(currentPrice)
                            .savings(savings)
                            .discountPercentage(discountPercentage)
                            .bestDeal(item.getProduct().getId().equals(bestDealId))
                            .build();
                })
                .collect(Collectors.toList());

        return WishlistPriceComparisonDto.builder()
                .totalItems(items.size())
                .totalOriginalPrice(totalOriginalPrice)
                .totalCurrentPrice(totalCurrentPrice)
                .totalSavings(totalSavings)
                .averageDiscount(averageDiscount)
                .items(comparisonItems)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public WishlistCostSummaryDto getWishlistCost(Long userId) {
        log.info("Calculating wishlist cost for user {}", userId);

        List<WishlistItem> items = wishlistRepository.findByUserIdOrderByCreatedAtDesc(userId);

        double subtotal = items.stream()
                .mapToDouble(item -> item.getProduct().getEffectivePrice().doubleValue() * item.getDesiredQuantity())
                .sum();

        // Estimated tax (10%)
        double estimatedTax = subtotal * 0.10;

        // Estimated shipping (5% or $10 minimum)
        double estimatedShipping = Math.max(subtotal * 0.05, 10.0);

        double totalCost = subtotal + estimatedTax + estimatedShipping;

        long inStockCount = items.stream()
                .filter(item -> item.getProduct().isInStock())
                .count();

        long outOfStockCount = items.stream()
                .filter(item -> item.getProduct().isOutOfStock())
                .count();

        // Group by priority
        Map<WishlistPriority, List<WishlistItem>> itemsByPriority = items.stream()
                .collect(Collectors.groupingBy(WishlistItem::getPriority));

        List<WishlistCostSummaryDto.PriorityBreakdown> priorityBreakdown = itemsByPriority.entrySet().stream()
                .map(entry -> {
                    double priorityCost = entry.getValue().stream()
                            .mapToDouble(item -> item.getProduct().getEffectivePrice().doubleValue() * item.getDesiredQuantity())
                            .sum();

                    return WishlistCostSummaryDto.PriorityBreakdown.builder()
                            .priority(entry.getKey().name())
                            .itemCount(entry.getValue().size())
                            .totalCost(priorityCost)
                            .build();
                })
                .collect(Collectors.toList());

        return WishlistCostSummaryDto.builder()
                .totalItems(items.size())
                .subtotal(subtotal)
                .estimatedTax(estimatedTax)
                .estimatedShipping(estimatedShipping)
                .totalCost(totalCost)
                .inStockItems((int) inStockCount)
                .outOfStockItems((int) outOfStockCount)
                .byPriority(priorityBreakdown)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<WishlistItemDto> getAvailableItems(Long userId) {
        return wishlistRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .filter(item -> item.getProduct().isInStock())
                .filter(item -> !item.getPurchased())
                .map(wishlistMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<WishlistItemDto> optimizeWishlist(Long userId, WishlistOptimizationRequest request) {
        log.info("Optimizing wishlist for user {} with strategy {}", userId, request.getOptimizationStrategy());

        List<WishlistItem> items = wishlistRepository.findByUserIdOrderByCreatedAtDesc(userId);

        // Filter by stock if requested
        if (Boolean.TRUE.equals(request.getIncludeOnlyInStock())) {
            items = items.stream()
                    .filter(item -> item.getProduct().isInStock())
                    .collect(Collectors.toList());
        }

        // Sort based on strategy
        switch (request.getOptimizationStrategy().toUpperCase()) {
            case "PRIORITY":
                items = sortByPriority(items, request.getPriorityOrder());
                break;
            case "PRICE":
                items = items.stream()
                        .sorted(Comparator.comparing(item -> item.getProduct().getEffectivePrice()))
                        .collect(Collectors.toList());
                break;
            case "SAVINGS":
                items = items.stream()
                        .filter(WishlistItem::isPriceDropped)
                        .sorted((a, b) -> b.getPriceDifference().compareTo(a.getPriceDifference()))
                        .collect(Collectors.toList());
                break;
            case "BALANCED":
            default:
                items = balancedSort(items);
                break;
        }

        // Apply budget constraint
        if (request.getMaxBudget() != null) {
            items = applyBudgetConstraint(items, request.getMaxBudget());
        }

        // Apply max items limit
        if (request.getMaxItems() != null && items.size() > request.getMaxItems()) {
            items = items.subList(0, request.getMaxItems());
        }

        return items.stream()
                .map(wishlistMapper::toDto)
                .collect(Collectors.toList());
    }

    // ==================== Private Helper Methods ====================

    private List<WishlistItem> sortByPriority(List<WishlistItem> items, List<String> priorityOrder) {
        if (priorityOrder == null || priorityOrder.isEmpty()) {
            priorityOrder = Arrays.asList("URGENT", "HIGH", "MEDIUM", "LOW");
        }

        Map<String, Integer> priorityMap = new HashMap<>();
        for (int i = 0; i < priorityOrder.size(); i++) {
            priorityMap.put(priorityOrder.get(i), i);
        }

        return items.stream()
                .sorted(Comparator.comparingInt(item ->
                        priorityMap.getOrDefault(item.getPriority().name(), Integer.MAX_VALUE)))
                .collect(Collectors.toList());
    }

    private List<WishlistItem> balancedSort(List<WishlistItem> items) {
        // Balanced sorting: Consider priority, price drops, and availability
        return items.stream()
                .sorted((a, b) -> {
                    // Priority weight
                    int priorityCompare = Integer.compare(
                            getPriorityWeight(b.getPriority()),
                            getPriorityWeight(a.getPriority())
                    );
                    if (priorityCompare != 0) return priorityCompare;

                    // Price drop weight
                    if (a.isPriceDropped() && !b.isPriceDropped()) return -1;
                    if (!a.isPriceDropped() && b.isPriceDropped()) return 1;

                    // Stock availability
                    if (a.getProduct().isInStock() && !b.getProduct().isInStock()) return -1;
                    if (!a.getProduct().isInStock() && b.getProduct().isInStock()) return 1;

                    // Finally, by price (lower first)
                    return a.getProduct().getEffectivePrice().compareTo(b.getProduct().getEffectivePrice());
                })
                .collect(Collectors.toList());
    }

    private int getPriorityWeight(WishlistPriority priority) {
        switch (priority) {
            case URGENT: return 4;
            case HIGH: return 3;
            case MEDIUM: return 2;
            case LOW: return 1;
            default: return 0;
        }
    }

    private List<WishlistItem> applyBudgetConstraint(List<WishlistItem> items, Double maxBudget) {
        List<WishlistItem> result = new ArrayList<>();
        double currentTotal = 0.0;

        for (WishlistItem item : items) {
            double itemCost = item.getProduct().getEffectivePrice().doubleValue() * item.getDesiredQuantity();
            if (currentTotal + itemCost <= maxBudget) {
                result.add(item);
                currentTotal += itemCost;
            } else {
                break;
            }
        }

        return result;
    }
}

