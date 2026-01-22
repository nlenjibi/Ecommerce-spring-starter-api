package com.smart_ecomernce_api.smart_ecomernce_api.modules.cart.dto;

import com.smart_ecomernce_api.Smart_ecommerce_api.modules.product.dto.ProductResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;



@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItemDto {
    private Long id;
    private ProductResponse product;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
}
