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
public class CartRecommendationsDto {

    @Builder.Default
    private List<RecommendedProduct> recommendations = new ArrayList<>();

    private String recommendationType; // FREQUENTLY_BOUGHT_TOGETHER, SIMILAR_ITEMS, COMPLETE_THE_LOOK

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecommendedProduct {
        private Long productId;
        private String name;
        private String description;
        private BigDecimal price;
        private String imageUrl;
        private Double score;
        private String reason;
    }
}
