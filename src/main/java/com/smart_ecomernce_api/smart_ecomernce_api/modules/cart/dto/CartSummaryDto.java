package com.smart_ecomernce_api.smart_ecomernce_api.modules.cart.dto;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartSummaryDto {
    private Long id;
    private String status;
    private Integer itemCount;
    private Integer uniqueItemCount;
    private BigDecimal subtotal;
    private BigDecimal discount;
    private BigDecimal totalPrice;
    private String couponCode;
}