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
public class SaveForLaterResult {
    private boolean success;
    private String message;
    private Integer itemsSaved;
    private Integer itemsFailed;
    private Long wishlistId;
}