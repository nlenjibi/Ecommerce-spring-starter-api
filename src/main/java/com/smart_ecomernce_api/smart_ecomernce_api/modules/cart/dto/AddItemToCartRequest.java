package com.smart_ecomernce_api.smart_ecomernce_api.modules.cart.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddItemToCartRequest {

    @Positive(message = "Cart ID must be positive")
    private Long cartId;

    @NotNull(message = "Product ID is required")
    @Positive(message = "Product ID must be positive")
    private Long productId;

    @Positive(message = "Quantity must be positive")
    @Builder.Default
    private Integer quantity = 1;
}
