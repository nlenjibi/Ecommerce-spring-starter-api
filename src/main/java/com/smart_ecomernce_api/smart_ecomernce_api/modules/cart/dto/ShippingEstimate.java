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
public class ShippingEstimate {
    private BigDecimal cost;
    private String currency;
    private Integer estimatedDays;
    private String method;
    private List<ShippingOption> availableOptions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ShippingOption {
        private String method;
        private String name;
        private BigDecimal cost;
        private Integer minDays;
        private Integer maxDays;
        private String description;
    }
}
