package com.smart_ecomernce_api.smart_ecomernce_api.modules.order.service;

import com.smart_ecomernce_api.smart_ecomernce_api.modules.order.dto.OrderCreateRequest;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.order.dto.OrderResponse;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.order.dto.OrderStatsResponse;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.order.dto.OrderUpdateRequest;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.order.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;

public interface OrderService {

        // Create & Retrieve
        OrderResponse createOrder(OrderCreateRequest request, Long userId);
        OrderResponse getOrderById(Long id, Long userId);
        OrderResponse getOrderByOrderNumber(String orderNumber, Long userId);

        // User Orders
        Page<OrderResponse> getUserOrders(Long userId, Pageable pageable);
        Page<OrderResponse> getUserOrdersByStatus(Long userId, OrderStatus status, Pageable pageable);

        // Admin - Get All Orders
        Page<OrderResponse> getAllOrders(Pageable pageable);
        Page<OrderResponse> getOrdersByStatus(OrderStatus status, Pageable pageable);

        // Update Operations
        OrderResponse updateOrderStatus(Long id, OrderUpdateRequest request);
        OrderResponse updatePaymentStatus(Long orderId, String status);
        OrderResponse updateOrderAsCustomer(Long id, OrderUpdateRequest request, Long userId);

        // Order Actions
        OrderResponse confirmOrder(Long id);
        OrderResponse shipOrder(Long id, String trackingNumber, String carrier);
        OrderResponse deliverOrder(Long id);
        OrderResponse cancelOrder(Long id, String reason, Long userId);
        OrderResponse refundOrder(Long id, BigDecimal amount, String reason);

        // Statistics
        OrderStatsResponse getOrderStatistics();

        void deleteOrder(Long orderId);

        OrderResponse getOrderByIdAsAdmin(Long id);
}
