package com.smart_ecomernce_api.smart_ecomernce_api.modules.cart.dto;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MergeCartRequest {

    @NotNull(message = "Guest cart ID is required")
    private Long guestCartId;
}
