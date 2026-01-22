package com.smart_ecomernce_api.smart_ecomernce_api.modules.cart.service.impl;

import com.smart_ecomernce_api.Smart_ecommerce_api.exception.CartNotFoundException;
import com.smart_ecomernce_api.Smart_ecommerce_api.exception.InsufficientStockException;
import com.smart_ecomernce_api.Smart_ecommerce_api.exception.ResourceNotFoundException;
import com.smart_ecomernce_api.Smart_ecommerce_api.modules.cart.dto.AddItemToCartRequest;
import com.smart_ecomernce_api.Smart_ecommerce_api.modules.cart.dto.CartDto;
import com.smart_ecomernce_api.Smart_ecommerce_api.modules.cart.dto.CartItemDto;
import com.smart_ecomernce_api.Smart_ecommerce_api.modules.cart.dto.UpdateCartItemRequest;
import com.smart_ecomernce_api.Smart_ecommerce_api.modules.cart.entity.Cart;
import com.smart_ecomernce_api.Smart_ecommerce_api.modules.cart.entity.CartItem;
import com.smart_ecomernce_api.Smart_ecommerce_api.modules.cart.entity.CartStatus;
import com.smart_ecomernce_api.Smart_ecommerce_api.modules.cart.mapper.CartMapper;
import com.smart_ecomernce_api.Smart_ecommerce_api.modules.cart.repository.CartRepository;
import com.smart_ecomernce_api.Smart_ecommerce_api.modules.cart.service.CartService;
import com.smart_ecomernce_api.Smart_ecommerce_api.modules.product.entity.Product;
import com.smart_ecomernce_api.Smart_ecommerce_api.modules.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final CartMapper cartMapper;

    @Override
    @CacheEvict(value = "carts", allEntries = true)
    public CartDto createCart() {
        log.info("Creating new cart");

        Cart cart = Cart.builder().status(CartStatus.ACTIVE).build();

        Cart savedCart = cartRepository.save(cart);

        log.info("Cart created with ID: {}", savedCart.getId());
        return cartMapper.toDto(savedCart);
    }

    @Override
    @CacheEvict(value = "carts", allEntries = true)
    public CartDto createCartForSession(String sessionId) {
        log.info("Creating cart for session: {}", sessionId);

        Cart cart = Cart.builder().sessionId(sessionId).status(CartStatus.ACTIVE).build();

        Cart savedCart = cartRepository.save(cart);

        log.info("Cart created with ID: {} for session: {}", savedCart.getId(), sessionId);
        return cartMapper.toDto(savedCart);
    }

    @Override
    @Cacheable(value = "carts", key = "#cartId")
    @Transactional(readOnly = true)
    public CartDto getCart(UUID cartId) {
        log.info("Fetching cart: {}", cartId);

        Cart cart = cartRepository.findByIdWithItems(cartId).orElseThrow(() -> new CartNotFoundException(cartId));

        return cartMapper.toDto(cart);
    }

    @Override
    @CacheEvict(value = "carts", key = "#cartId")
    public CartItemDto addToCart(UUID cartId, AddItemToCartRequest request) {
        log.info("Adding product {} to cart {}", request.getProductId(), cartId);

        Cart cart = cartRepository.findByIdWithItems(cartId).orElseThrow(() -> new CartNotFoundException(cartId));

        Product product = productRepository.findById(request.getProductId()).orElseThrow(() -> ResourceNotFoundException.forResource("Product", request.getProductId()));

        int requestedQuantity = request.getQuantity() != null ? request.getQuantity() : 1;
        if (product.getStockQuantity() < requestedQuantity) {
            throw new InsufficientStockException(product.getName(), product.getStockQuantity(), requestedQuantity);
        }

        CartItem cartItem;
        if (request.getQuantity() != null && request.getQuantity() > 1) {
            cartItem = cart.addItem(product, request.getQuantity());
        } else {
            cartItem = cart.addItem(product);
        }

        cartRepository.save(cart);

        log.info("Added product {} to cart {} with quantity {}", product.getId(), cartId, cartItem.getQuantity());

        return cartMapper.toDto(cartItem);
    }

    @Override
    @CacheEvict(value = "carts", key = "#cartId")
    public CartItemDto updateItemQuantity(UUID cartId, Long productId, UpdateCartItemRequest request) {
        log.info("Updating cart {} item {} quantity to {}", cartId, productId, request.getQuantity());

        Cart cart = cartRepository.findByIdWithItems(cartId).orElseThrow(() -> new CartNotFoundException(cartId));

        CartItem cartItem = cart.getItem(productId);
        if (cartItem == null) {
            ResourceNotFoundException.forResource("Cart item for product", productId);
        }

        Product product = cartItem.getProduct();
        if (product.getStockQuantity() < request.getQuantity()) {
            throw new InsufficientStockException(product.getName(), product.getStockQuantity(), request.getQuantity());
        }

        cartItem.setQuantity(request.getQuantity());
        cartRepository.save(cart);

        log.info("Updated cart item quantity: cart={}, product={}, quantity={}", cartId, productId, request.getQuantity());

        return cartMapper.toDto(cartItem);
    }

    @Override
    @CacheEvict(value = "carts", key = "#cartId")
    public void removeItem(UUID cartId, Long productId) {
        log.info("Removing product {} from cart {}", productId, cartId);

        Cart cart = cartRepository.findByIdWithItems(cartId).orElseThrow(() -> new CartNotFoundException(cartId));

        cart.removeItem(productId);
        cartRepository.save(cart);

        log.info("Removed product {} from cart {}", productId, cartId);
    }

    @Override
    @CacheEvict(value = "carts", key = "#cartId")
    public void clearCart(UUID cartId) {
        log.info("Clearing cart: {}", cartId);

        Cart cart = cartRepository.findByIdWithItems(cartId).orElseThrow(() -> new CartNotFoundException(cartId));

        if (cart.isEmpty()) {
            throw new CartNotFoundException("Cart is already empty");
        }

        cart.clear();
        cartRepository.save(cart);

        log.info("Cart cleared: {}", cartId);
    }

    @Override
    @CacheEvict(value = "carts", key = "#cartId")
    public CartDto applyDiscount(UUID cartId, String couponCode, BigDecimal discountAmount) {
        log.info("Applying discount to cart {}: code={}, amount={}", cartId, couponCode, discountAmount);

        Cart cart = cartRepository.findByIdWithItems(cartId).orElseThrow(() -> new CartNotFoundException(cartId));

        cart.applyDiscount(couponCode, discountAmount);
        Cart savedCart = cartRepository.save(cart);

        log.info("Discount applied to cart: {}", cartId);
        return cartMapper.toDto(savedCart);
    }

    @Override
    @CacheEvict(value = "carts", key = "#cartId")
    public void markAsAbandoned(UUID cartId) {
        log.info("Marking cart as abandoned: {}", cartId);

        Cart cart = cartRepository.findById(cartId).orElseThrow(() -> new CartNotFoundException(cartId));

        cart.markAsAbandoned();
        cartRepository.save(cart);

        log.info("Cart marked as abandoned: {}", cartId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CartDto> getAbandonedCarts(int hours) {
        log.info("Fetching abandoned carts older than {} hours", hours);

        LocalDateTime cutoffDate = LocalDateTime.now().minusHours(hours);
        List<Cart> abandonedCarts = cartRepository.findAbandonedCartsBefore(cutoffDate);

        log.info("Found {} abandoned carts", abandonedCarts.size());
        return cartMapper.toDtoList(abandonedCarts);
    }

    @Override
    @CacheEvict(value = "carts", allEntries = true)
    public int cleanupOldCarts(int days) {
        log.info("Cleaning up empty carts older than {} days", days);

        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(days);
        List<Cart> oldCarts = cartRepository.findEmptyCartsBefore(cutoffDate);

        cartRepository.deleteAll(oldCarts);

        log.info("Deleted {} old empty carts", oldCarts.size());
        return oldCarts.size();
    }
}