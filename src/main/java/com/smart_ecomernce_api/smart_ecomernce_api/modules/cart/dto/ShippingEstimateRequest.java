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
public class ShippingEstimateRequest {

    @NotBlank(message = "Country is required")
    private String country;

    @NotBlank(message = "State/Province is required")
    private String state;

    @NotBlank(message = "Postal code is required")
    private String postalCode;

    private String city;
    private String shippingMethod; // STANDARD, EXPRESS, OVERNIGHT
}