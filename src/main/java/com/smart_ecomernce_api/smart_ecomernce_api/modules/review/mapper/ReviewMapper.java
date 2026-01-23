package com.smart_ecomernce_api.smart_ecomernce_api.modules.review.mapper;

import com.smart_ecomernce_api.smart_ecomernce_api.modules.review.dto.ReviewCreateRequest;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.review.dto.ReviewResponse;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.review.dto.ReviewUpdateRequest;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.review.entity.Review;
import org.mapstruct.*;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder =false ))
public interface ReviewMapper {

    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "productName", source = "product.name")
    @Mapping(target = "user.id", source = "user.id")
    @Mapping(target = "user.firstName", source = "user.firstName")
    @Mapping(target = "user.lastName", source = "user.lastName")
    @Mapping(target = "user.email", source = "user.email")
    ReviewResponse toDto(Review review);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "product", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "verifiedPurchase", ignore = true)
    @Mapping(target = "approved", constant = "false")
    @Mapping(target = "helpfulCount", constant = "0")
    @Mapping(target = "notHelpfulCount", constant = "0")
    Review toEntity(ReviewCreateRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "product", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "verifiedPurchase", ignore = true)
    @Mapping(target = "approved", ignore = true)
    @Mapping(target = "helpfulCount", ignore = true)
    @Mapping(target = "notHelpfulCount", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void update(ReviewUpdateRequest request, @MappingTarget Review review);
}
