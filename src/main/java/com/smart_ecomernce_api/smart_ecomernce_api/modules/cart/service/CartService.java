package com.smart_ecomernce_api.smart_ecomernce_api.modules.cart.service;



import com.smart_ecomernce_api.smart_ecomernce_api.modules.cart.dto.AddItemToCartRequest;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.cart.dto.CartDto;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.cart.dto.CartItemDto;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.cart.dto.UpdateCartItemRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface CartService {
    CartDto createCart();
    CartDto createCartForSession(String sessionId);
    CartDto getCart(UUID cartId);
    CartItemDto addToCart(UUID cartId, AddItemToCartRequest request);
    CartItemDto updateItemQuantity(UUID cartId, Long productId, UpdateCartItemRequest request);
    void removeItem(UUID cartId, Long productId);
    void clearCart(UUID cartId);
    CartDto applyDiscount(UUID cartId, String couponCode, BigDecimal discountAmount);
    void markAsAbandoned(UUID cartId);
    List<CartDto> getAbandonedCarts(int hours);
    int cleanupOldCarts(int days);
}
