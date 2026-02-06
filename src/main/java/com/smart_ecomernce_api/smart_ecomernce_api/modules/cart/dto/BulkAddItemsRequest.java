package com.smart_ecomernce_api.smart_ecomernce_api.modules.cart.dto;


import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request to add multiple items to cart at once
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkAddItemsRequest {

    @NotEmpty(message = "Items list cannot be empty")
    @Size(max = 50, message = "Cannot add more than 50 items at once")
    @Valid
    private List<AddItemToCartRequest> items;
}