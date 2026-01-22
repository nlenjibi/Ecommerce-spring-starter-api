package com.smart_ecomernce_api.smart_ecomernce_api.modules.product.mapper;

import com.smart_ecomernce_api.Smart_ecommerce_api.modules.product.dto.ProductCreateRequest;
import com.smart_ecomernce_api.Smart_ecommerce_api.modules.product.dto.ProductResponse;
import com.smart_ecomernce_api.Smart_ecommerce_api.modules.product.dto.ProductUpdateRequest;
import com.smart_ecomernce_api.Smart_ecommerce_api.modules.product.entity.Product;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ProductMapper {
    ProductResponse toDto(Product product);

    Product toEntity(ProductCreateRequest request);

    @Mapping(target = "id", ignore = true)
    void update(ProductUpdateRequest request, @MappingTarget Product product);
}