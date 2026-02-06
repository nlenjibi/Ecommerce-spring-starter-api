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
public class CartValidationResult {

    private boolean valid;
    private String message;

    @Builder.Default
    private List<ValidationIssue> issues = new ArrayList<>();

    private BigDecimal originalTotal;
    private BigDecimal updatedTotal;
    private boolean priceChanged;
    private boolean stockChanged;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ValidationIssue {
        private String type; // OUT_OF_STOCK, INSUFFICIENT_STOCK, PRICE_CHANGED, ITEM_UNAVAILABLE
        private Long productId;
        private String productName;
        private String message;
        private Integer requestedQuantity;
        private Integer availableQuantity;
        private BigDecimal oldPrice;
        private BigDecimal newPrice;
    }
}
