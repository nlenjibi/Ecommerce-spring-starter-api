package com.smart_ecomernce_api.smart_ecomernce_api.modules.cart.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartDto {
    private UUID id;
    private LocalDateTime dateCreated;
    private LocalDateTime updatedAt;
    private String status;

    @Builder.Default
    private List<CartItemDto> items = new ArrayList<>();

    private Integer itemCount;
    private BigDecimal subtotal;
    private BigDecimal discount;
    private BigDecimal totalPrice;

    private String couponCode;
}
