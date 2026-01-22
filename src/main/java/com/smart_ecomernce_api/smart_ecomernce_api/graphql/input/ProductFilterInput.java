package com.smart_ecomernce_api.smart_ecomernce_api.graphql.input;

import lombok.*;

import java.math.BigDecimal;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Getter
@Setter
public class ProductFilterInput {
    private Long categoryId;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private String search;
    private Boolean featured;
    private Boolean isNew;
    private Boolean inStock;

}
