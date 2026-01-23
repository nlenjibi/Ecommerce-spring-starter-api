package com.smart_ecomernce_api.smart_ecomernce_api.graphql.resolver;

import com.smart_ecomernce_api.smart_ecomernce_api.modules.cart.dto.AddItemToCartRequest;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.cart.dto.CartDto;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.cart.dto.CartItemDto;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.cart.dto.UpdateCartItemRequest;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.cart.service.CartService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.UUID;

@Controller
@RequiredArgsConstructor
@Slf4j
public class CartResolver {

    private final CartService cartService;

    @QueryMapping
    @Cacheable(value = "carts", key = "#id")
    public CartDto cart(@Argument UUID id) {
        log.info("GraphQL Query: cart(id: {})", id);
        return cartService.getCart(id);
    }

    @MutationMapping
    @CacheEvict(value = "carts", allEntries = true)
    public CartDto createCart() {
        log.info("GraphQL Mutation: createCart");
        return cartService.createCart();
    }

    @MutationMapping
    @CacheEvict(value = "carts", key = "#cartId", condition = "#cartId != null")
    public CartItemDto addItemToCart(@Argument UUID cartId, @Argument AddItemToCartRequest input) {
        log.info("GraphQL Mutation: addItemToCart(cartId: {})", cartId);
        return cartService.addToCart(cartId, input);
    }

    @MutationMapping
    @CacheEvict(value = "carts", key = "#cartId", condition = "#cartId != null")
    public CartItemDto updateCartItem(
            @Argument UUID cartId,
            @Argument Long productId,
            @Argument UpdateCartItemRequest input) {
        log.info("GraphQL Mutation: updateCartItem(cartId: {}, productId: {})", cartId, productId);
        return cartService.updateItemQuantity(cartId, productId, input);
    }

    @MutationMapping
    @CacheEvict(value = "carts", key = "#cartId", condition = "#cartId != null")
    public Boolean removeCartItem(@Argument UUID cartId, @Argument Long productId) {
        log.info("GraphQL Mutation: removeCartItem(cartId: {}, productId: {})", cartId, productId);
        cartService.removeItem(cartId, productId);
        return true;
    }

    @MutationMapping
    @CacheEvict(value = "carts", key = "#cartId", condition = "#cartId != null")
    public Boolean clearCart(@Argument UUID cartId) {
        log.info("GraphQL Mutation: clearCart(cartId: {})", cartId);
        cartService.clearCart(cartId);
        return true;
    }
}