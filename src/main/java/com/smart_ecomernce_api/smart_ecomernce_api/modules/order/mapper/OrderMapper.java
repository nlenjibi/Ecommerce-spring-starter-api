package com.smart_ecomernce_api.smart_ecomernce_api.modules.order.mapper;

import com.smart_ecomernce_api.smart_ecomernce_api.modules.order.dto.OrderItemResponse;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.order.dto.OrderResponse;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.order.entity.Order;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.order.entity.OrderItem;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = false))
public interface OrderMapper {

    @Mapping(target = "userId", source = "user.id")

    @Mapping(target = "items", source = "orderItems")
    @Mapping(target = "itemCount", expression = "java(order.getItemCount())")
    OrderResponse toDto(Order order);

    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "productName", source = "productName")
    @Mapping(target = "totalPrice", expression = "java(orderItem.getTotalPrice())")
    OrderItemResponse toItemDto(OrderItem orderItem);

    List<OrderResponse> toDtoList(List<Order> orders);
    List<OrderItemResponse> toItemDtoList(List<OrderItem> items);
}