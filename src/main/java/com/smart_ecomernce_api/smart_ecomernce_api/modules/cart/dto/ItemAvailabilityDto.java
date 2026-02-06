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
public class ItemAvailabilityDto {
    private Long productId;
    private String productName;
    private boolean available;
    private Integer requestedQuantity;
    private Integer availableQuantity;
    private boolean priceChanged;
    private BigDecimal currentPrice;
    private String message;
}