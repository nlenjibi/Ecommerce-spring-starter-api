package com.smart_ecomernce_api.smart_ecomernce_api.modules.cart.controller;

import com.smart_ecomernce_api.smart_ecomernce_api.modules.cart.dto.AddItemToCartRequest;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.cart.dto.CartDto;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.cart.dto.CartItemDto;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.cart.dto.UpdateCartItemRequest;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.cart.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.UUID;

/**
 * Cart REST Controller
 * Provides endpoints for shopping cart management
 */
@RestController
@RequestMapping("v1/carts")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Cart", description = "Shopping cart management API")
public class CartController {

    private final CartService cartService;

    /**
     * Create a new cart
     */
    @PostMapping
    @Operation(summary = "Create new cart", description = "Creates a new empty shopping cart")
    public ResponseEntity<CartDto> createCart(UriComponentsBuilder uriBuilder) {
        log.debug("POST /carts - Creating new cart");

        CartDto cartDto = cartService.createCart();

        var uri = uriBuilder.path("/carts/{id}")
                .buildAndExpand(cartDto.getId())
                .toUri();

        return ResponseEntity.created(uri).body(cartDto);
    }

    /**
     * Get cart by ID
     */
    @GetMapping("/{cartId}")
    @Operation(summary = "Get cart", description = "Retrieves cart by ID with all items")
    public ResponseEntity<CartDto> getCart(
            @Parameter(description = "Cart ID") @PathVariable UUID cartId
    ) {
        log.debug("GET /carts/{} - Fetching cart", cartId);

        CartDto cartDto = cartService.getCart(cartId);

        return ResponseEntity.ok(cartDto);
    }

    /**
     * Add item to cart
     */
    @PostMapping("/{cartId}/items")
    @Operation(summary = "Add item to cart", description = "Adds a product to the cart")
    public ResponseEntity<CartItemDto> addItemToCart(
            @Parameter(description = "Cart ID") @PathVariable UUID cartId,
            @Valid @RequestBody AddItemToCartRequest request
    ) {
        log.debug("POST /carts/{}/items - Adding product {}", cartId, request.getProductId());

        CartItemDto cartItemDto = cartService.addToCart(cartId, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(cartItemDto);
    }

    /**
     * Update cart item quantity
     */
    @PutMapping("/{cartId}/items/{productId}")
    @Operation(summary = "Update item quantity", description = "Updates the quantity of a cart item")
    public ResponseEntity<CartItemDto> updateCartItem(
            @Parameter(description = "Cart ID") @PathVariable UUID cartId,
            @Parameter(description = "Product ID") @PathVariable Long productId,
            @Valid @RequestBody UpdateCartItemRequest request
    ) {
        log.debug("PUT /carts/{}/items/{} - Updating quantity to {}",
                cartId, productId, request.getQuantity());

        CartItemDto cartItemDto = cartService.updateItemQuantity(cartId, productId, request);

        return ResponseEntity.ok(cartItemDto);
    }

    /**
     * Remove item from cart
     */
    @DeleteMapping("/{cartId}/items/{productId}")
    @Operation(summary = "Remove item", description = "Removes an item from the cart")
    public ResponseEntity<Void> removeItemFromCart(
            @Parameter(description = "Cart ID") @PathVariable UUID cartId,
            @Parameter(description = "Product ID") @PathVariable Long productId
    ) {
        log.debug("DELETE /carts/{}/items/{} - Removing item", cartId, productId);

        cartService.removeItem(cartId, productId);

        return ResponseEntity.noContent().build();
    }

    /**
     * Clear all items from cart
     */
    @DeleteMapping("/{cartId}/items")
    @Operation(summary = "Clear cart", description = "Removes all items from the cart")
    public ResponseEntity<Void> clearCart(
            @Parameter(description = "Cart ID") @PathVariable UUID cartId
    ) {
        log.debug("DELETE /carts/{}/items - Clearing cart", cartId);

        cartService.clearCart(cartId);

        return ResponseEntity.noContent().build();
    }


}
