package com.smart_ecomernce_api.smart_ecomernce_api.modules.user.mapper;


import com.smart_ecomernce_api.Smart_ecommerce_api.modules.product.dto.WishlistItemDto;
import com.smart_ecomernce_api.Smart_ecommerce_api.modules.product.entity.WishlistItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface WishlistMapper {

    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "product", target = "product", qualifiedByName = "toProductSummary")
    @Mapping(source = "createdAt", target = "addedAt")
    @Mapping(target = "currentPrice", expression = "java(item.getProduct().getEffectivePrice())")
    @Mapping(target = "priceDifference", expression = "java(item.getPriceDifference())")
    @Mapping(target = "isPriceDropped", expression = "java(item.isPriceDropped())")
    @Mapping(target = "shouldNotifyPriceDrop", expression = "java(item.shouldNotifyPriceDrop())")
    @Mapping(target = "shouldNotifyStock", expression = "java(item.shouldNotifyStock())")
    @Mapping(target = "inStock", expression = "java(item.getProduct().isInStock())")
    WishlistItemDto toDto(WishlistItem item);

    @Named("toProductSummary")
    default WishlistItemDto.ProductSummary toProductSummary(
            com.smart_ecomernce_api.Smart_ecommerce_api.modules.product.entity.Product product) {
        if (product == null) {
            return null;
        }

        return WishlistItemDto.ProductSummary.builder()
                .id(product.getId())
                .name(product.getName())
                .slug(product.getSlug())
                .sku(product.getSku())
                .price(product.getPrice())
                .discountPrice(product.getDiscountPrice())
                .imageUrl(product.getImageUrl())
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                .inStock(product.isInStock())
                .availableQuantity(product.getAvailableQuantity())
                .inventoryStatus(product.getInventoryStatus() != null
                        ? product.getInventoryStatus().name()
                        : null)
                .build();
    }
}