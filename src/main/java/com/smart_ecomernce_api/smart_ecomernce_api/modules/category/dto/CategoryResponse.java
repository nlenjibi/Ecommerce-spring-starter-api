package com.smart_ecomernce_api.smart_ecomernce_api.modules.category.dto;


import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CategoryResponse {
    private Long id;
    private String slug;
    private String name;
    private String description;
    private String imageUrl;
    private Integer displayOrder;
    private Integer level;
    private Boolean isActive;
    private ParentCategoryInfo parent;
    private List<CategoryResponse> children;
    private Long productCount;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParentCategoryInfo {
        private Long id;
        private String slug;
        private String name;
    }
}