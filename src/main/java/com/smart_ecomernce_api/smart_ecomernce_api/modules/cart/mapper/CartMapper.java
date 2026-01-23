package com.smart_ecomernce_api.smart_ecomernce_api.modules.cart.mapper;

import com.smart_ecomernce_api.smart_ecomernce_api.modules.cart.dto.CartDto;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.cart.dto.CartItemDto;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.cart.entity.Cart;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.cart.entity.CartItem;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.product.dto.ProductResponse;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.product.entity.Product;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import java.util.List;


@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface CartMapper {

    @Mapping(target = "itemCount", expression = "java(cart.getItemCount())")
    @Mapping(target = "subtotal", expression = "java(cart.getTotalPrice())")
    @Mapping(target = "discount", source = "discountAmount")
    @Mapping(target = "totalPrice", expression = "java(cart.getFinalPrice())")
    @Mapping(target = "status", expression = "java(cart.getStatus().name())")
    @Mapping(target = "items", source = "items")  // FIXED: Map items correctly
    CartDto toDto(Cart cart);

    List<CartDto> toDtoList(List<Cart> carts);

    @Mapping(target = "totalPrice", expression = "java(cartItem.getTotalPrice())")
    CartItemDto toDto(CartItem cartItem);

    List<CartItemDto> toCartItemDtoList(List<CartItem> cartItems);

    @Mapping(target = "inStock", expression = "java(product.getStockQuantity() != null && product.getStockQuantity() > 0)")
    ProductResponse toProductDto(Product product);
}
