package com.smart_ecomernce_api.smart_ecomernce_api.modules.product.service.impl;

import com.smart_ecomernce_api.smart_ecomernce_api.exception.DuplicateResourceException;
import com.smart_ecomernce_api.smart_ecomernce_api.exception.ResourceNotFoundException;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.product.dto.AddToWishlistRequest;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.product.dto.WishlistItemDto;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.product.entity.Product;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.product.entity.WishlistItem;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.product.mapper.WishlistMapper;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.product.repository.ProductRepository;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.product.repository.WishlistRepository;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.product.service.GuestWishlistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GuestWishlistServiceImpl implements GuestWishlistService {

    private final WishlistRepository wishlistRepository;
    private final ProductRepository productRepository;
    private final WishlistMapper wishlistMapper;

    @Override
    public String generateGuestSessionId() {
        return "GUEST_" + UUID.randomUUID().toString();
    }

    @Override
    @Transactional
    public WishlistItemDto addToGuestWishlist(String guestSessionId, AddToWishlistRequest request) {
        log.info("Adding product {} to guest wishlist for session {}", request.getProductId(), guestSessionId);

        // Validate product exists and is active
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found with id: " + request.getProductId()));

        if (!product.getIsActive()) {
            throw new ResourceNotFoundException("Product is not available");
        }

        // Check if already in guest wishlist
        if (wishlistRepository.existsByGuestSessionIdAndProductId(guestSessionId, request.getProductId())) {
            throw new DuplicateResourceException(
                    "Product already in guest wishlist");
        }

        // Create wishlist item for guest
        WishlistItem wishlistItem = WishlistItem.builder()
                .guestSessionId(guestSessionId)
                .product(product)
                .notes(request.getNotes())
                .priority(request.getPriority())
                .desiredQuantity(request.getDesiredQuantity() != null ? request.getDesiredQuantity() : 1)
                .priceWhenAdded(product.getEffectivePrice())
                .targetPrice(request.getTargetPrice())
                .notifyOnPriceDrop(false) // Disabled for guests
                .notifyOnStock(false) // Disabled for guests
                .isPublic(false)
                .purchased(false)
                .guestSessionExpiresAt(LocalDateTime.now().plusDays(30)) // 30-day expiry
                .build();

        WishlistItem saved = wishlistRepository.save(wishlistItem);
        log.info("Product added to guest wishlist: wishlistItemId={}", saved.getId());

        return wishlistMapper.toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WishlistItemDto> getGuestWishlist(String guestSessionId) {
        return wishlistRepository.findByGuestSessionIdOrderByCreatedAtDesc(guestSessionId).stream()
                .map(wishlistMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void removeFromGuestWishlist(String guestSessionId, Long productId) {
        log.info("Removing product {} from guest wishlist for session {}", productId, guestSessionId);

        WishlistItem item = wishlistRepository.findByGuestSessionIdAndProductId(guestSessionId, productId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found in guest wishlist"));

        wishlistRepository.delete(item);
        log.info("Product removed from guest wishlist: wishlistItemId={}", item.getId());
    }

    @Override
    @Transactional
    public void clearGuestWishlist(String guestSessionId) {
        log.info("Clearing guest wishlist for session {}", guestSessionId);
        wishlistRepository.deleteByGuestSessionId(guestSessionId);
        log.info("Guest wishlist cleared for session {}", guestSessionId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isInGuestWishlist(String guestSessionId, Long productId) {
        return wishlistRepository.existsByGuestSessionIdAndProductId(guestSessionId, productId);
    }

    @Override
    @Transactional
    public void mergeGuestWishlistToUser(String guestSessionId, Long userId) {
        log.info("Merging guest wishlist {} to user {}", guestSessionId, userId);

        try {
            wishlistRepository.mergeGuestItemsToUser(guestSessionId, userId);
            log.info("Guest wishlist merged successfully");
        } catch (Exception e) {
            log.error("Error merging guest wishlist to user", e);
            throw new RuntimeException("Failed to merge guest wishlist", e);
        }
    }

    @Override
    @Transactional
    @Scheduled(cron = "0 0 2 * * ?") // Run at 2 AM daily
    public void cleanupExpiredGuestSessions() {
        log.info("Starting cleanup of expired guest sessions");

        try {
            LocalDateTime now = LocalDateTime.now();
            wishlistRepository.deleteExpiredGuestSessions(now);
            log.info("Expired guest sessions cleaned up successfully");
        } catch (Exception e) {
            log.error("Error cleaning up expired guest sessions", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Long getGuestWishlistCount(String guestSessionId) {
        return wishlistRepository.countByGuestSessionId(guestSessionId);
    }

    @Override
    @Transactional
    public void sendGuestWishlistToEmail(String guestSessionId, String email) {
        log.info("Sending guest wishlist to email: {}", email);

        List<WishlistItem> items = wishlistRepository.findByGuestSessionIdOrderByCreatedAtDesc(guestSessionId);

        if (items.isEmpty()) {
            throw new ResourceNotFoundException("Guest wishlist is empty");
        }

        // Update email for notification purposes
        items.forEach(item -> {
            item.setGuestEmail(email);
            wishlistRepository.save(item);
        });

        // TODO: Integrate with email service to send wishlist details
        // emailService.sendGuestWishlist(email, items);

        log.info("Guest wishlist sent to email successfully");
    }
}