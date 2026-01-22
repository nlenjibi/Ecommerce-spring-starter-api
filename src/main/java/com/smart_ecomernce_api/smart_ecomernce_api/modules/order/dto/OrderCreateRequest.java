package com.smart_ecomernce_api.smart_ecomernce_api.modules.order.dto;

import com.smart_ecomernce_api.Smart_ecommerce_api.modules.order.entity.PaymentMethod;
import com.smart_ecomernce_api.Smart_ecommerce_api.modules.order.entity.ShippingMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderCreateRequest {

    @NotEmpty(message = "Order items cannot be empty")
    @Valid
    private List<OrderItemRequest> items;

    @NotNull(message = "Shipping method is required")
    private ShippingMethod shippingMethod;

    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;
    private String customerEmail;
    private String customerName;
    private String couponCode;
    private BigDecimal taxRate;

    private String customerNotes;
    public BigDecimal couponDiscount ;

}