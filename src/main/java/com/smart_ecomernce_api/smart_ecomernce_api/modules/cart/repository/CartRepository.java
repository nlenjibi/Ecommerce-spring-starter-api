package com.smart_ecomernce_api.smart_ecomernce_api.modules.cart.repository;

import com.smart_ecomernce_api.smart_ecomernce_api.modules.cart.entity.Cart;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.cart.entity.CartItem;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.cart.entity.CartStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Custom JDBC Repository for Cart operations
 * Provides raw SQL access for complex queries and better performance control
 */
public interface CartRepository {


    Cart save(Cart cart);


    Cart update(Cart cart);


    Optional<Cart> findById(UUID id);


    Optional<Cart> findByIdWithItems(UUID id);


    List<Cart> findAbandonedCartsBefore(LocalDateTime cutoffDate);



    List<Cart> findEmptyCartsBefore(LocalDateTime cutoffDate);


    boolean deleteById(UUID id);


    CartItem saveCartItem(CartItem cartItem);



    List<CartItem> findCartItemsByCartId(UUID cartId);


    int deleteCartItemsByCartId(UUID cartId);


    long count();


    boolean existsById(UUID id);


    List<Cart> findAll(int limit, int offset);
}