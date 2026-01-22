package com.smart_ecomernce_api.smart_ecomernce_api.modules.review.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewResponse {
    private Long id;
    private Long productId;
    private String productName;
    private UserInfo user;
    private Integer rating;
    private String title;
    private String comment;
    private Boolean verifiedPurchase;
    private Boolean approved;
    private Integer helpfulCount;
    private Integer notHelpfulCount;
    private List<String> images;
    private List<String> pros;
    private List<String> cons;
    private String adminResponse;
    private String createdAt;
    private String updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInfo {
        private Long id;
        private String firstName;
        private String lastName;
        private String email;
    }
}
