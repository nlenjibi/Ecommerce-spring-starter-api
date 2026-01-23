package com.smart_ecomernce_api.smart_ecomernce_api.graphql.resolver;


import com.smart_ecomernce_api.smart_ecomernce_api.graphql.input.PageInput;
import com.smart_ecomernce_api.smart_ecomernce_api.graphql.input.SortDirection;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.order.dto.OrderCreateRequest;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.order.dto.OrderResponse;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.order.dto.OrderStatsResponse;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.ContextValue;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.math.BigDecimal;

@Controller
@RequiredArgsConstructor
@Slf4j
class OrderResolver {

    private final OrderService orderService;

    @QueryMapping
    @Cacheable(value = "orders", key = "#id + '_' + #userId")
    public OrderResponse order(@Argument Long id, @ContextValue Long userId) {
        log.info("GraphQL Query: order(id: {})", id);
        return orderService.getOrderById(id, userId);
    }

    @QueryMapping
    @Cacheable(value = "orders", key = "#orderNumber + '_' + #userId")
    public OrderResponse orderByNumber(@Argument String orderNumber, @ContextValue Long userId) {
        log.info("GraphQL Query: orderByNumber(orderNumber: {})", orderNumber);
        return orderService.getOrderByOrderNumber(orderNumber, userId);
    }

    @QueryMapping
    @Cacheable(value = "orders", key = "'user_' + #userId + '_' + (#pagination==null?0:#pagination.page) + '_' + (#pagination==null?10:#pagination.size)")
    public Page<OrderResponse> myOrders(@Argument PageInput pagination, @ContextValue Long userId) {
        log.info("GraphQL Query: myOrders for user {}", userId);
        Pageable pageable = createPageable(pagination);
        return orderService.getUserOrders(userId, pageable);
    }

    @QueryMapping
    @Cacheable(value = "orders", key = "'all_' + (#pagination==null?0:#pagination.page) + '_' + (#pagination==null?10:#pagination.size)")
    public Page<OrderResponse> allOrders(@Argument PageInput pagination) {
        log.info("GraphQL Query: allOrders");
        Pageable pageable = createPageable(pagination);
        return orderService.getAllOrders(pageable);
    }

    @QueryMapping
    @Cacheable(value = "orders", key = "'stats'")
    public OrderStatsResponse orderStatistics() {
        log.info("GraphQL Query: orderStatistics");
        return orderService.getOrderStatistics();
    }

    @MutationMapping
    @CacheEvict(value = "orders", allEntries = true)
    public OrderResponse createOrder(@Argument OrderCreateRequest input, @ContextValue Long userId) {
        log.info("GraphQL Mutation: createOrder for user {}", userId);
        return orderService.createOrder(input, userId);
    }

    @MutationMapping
    @CacheEvict(value = "orders", allEntries = true)
    public OrderResponse cancelOrder(@Argument Long id, @Argument String reason, @ContextValue Long userId) {
        log.info("GraphQL Mutation: cancelOrder(id: {})", id);
        return orderService.cancelOrder(id, reason, userId);
    }

    @MutationMapping
    @CacheEvict(value = "orders", allEntries = true)
    public OrderResponse confirmOrder(@Argument Long id) {
        log.info("GraphQL Mutation: confirmOrder(id: {})", id);
        return orderService.confirmOrder(id);
    }

    @MutationMapping
    @CacheEvict(value = "orders", allEntries = true)
    public OrderResponse shipOrder(
            @Argument Long id,
            @Argument String trackingNumber,
            @Argument String carrier) {
        log.info("GraphQL Mutation: shipOrder(id: {})", id);
        return orderService.shipOrder(id, trackingNumber, carrier);
    }

    @MutationMapping
    @CacheEvict(value = "orders", allEntries = true)
    public OrderResponse deliverOrder(@Argument Long id) {
        log.info("GraphQL Mutation: deliverOrder(id: {})", id);
        return orderService.deliverOrder(id);
    }

    @MutationMapping
    @CacheEvict(value = "orders", allEntries = true)
    public OrderResponse refundOrder(
            @Argument Long id,
            @Argument BigDecimal amount,
            @Argument String reason) {
        log.info("GraphQL Mutation: refundOrder(id: {})", id);
        return orderService.refundOrder(id, amount, reason);
    }

    private Pageable createPageable(PageInput input) {
        if (input == null) {
            return PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "orderDate"));
        }
        Sort sort = input.getDirection() == SortDirection.DESC
                ? Sort.by(input.getSortBy()).descending()
                : Sort.by(input.getSortBy()).ascending();
        return PageRequest.of(input.getPage(), input.getSize(), sort);
    }
}
