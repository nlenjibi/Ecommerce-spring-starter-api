package com.smart_ecomernce_api.smart_ecomernce_api.modules.order.controller;


import com.smart_ecomernce_api.smart_ecomernce_api.common.response.ApiResponse;
import com.smart_ecomernce_api.smart_ecomernce_api.common.response.PaginatedResponse;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.order.dto.OrderCreateRequest;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.order.dto.OrderResponse;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.order.dto.OrderStatsResponse;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.order.dto.OrderUpdateRequest;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.order.entity.OrderStatus;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("v1/orders")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Orders", description = "Order management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class OrderController {

    private final OrderService orderService;


    @PostMapping
    @Operation(summary = "Create order", description = "Create a new order")
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @Valid @RequestBody OrderCreateRequest request,
            Long Id
    ) {
        OrderResponse response = orderService.createOrder(request, Id);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Order created successfully", response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get order by ID")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderById(
            @PathVariable Long id,
            Long userId) {
        OrderResponse response = orderService.getOrderById(id, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/number/{orderNumber}")
    @Operation(summary = "Get order by number")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderByNumber(
            @PathVariable String orderNumber, Long userId

    ) {

        OrderResponse response = orderService.getOrderByOrderNumber(orderNumber, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/my-orders")
    @Operation(summary = "Get my orders")
    public ResponseEntity<ApiResponse<PaginatedResponse<OrderResponse>>> getMyOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "orderDate") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction, Long userId

    ) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        Page<OrderResponse> orders = orderService.getUserOrders(userId, pageable);
        return ResponseEntity.ok(ApiResponse.success(PaginatedResponse.from(orders)));
    }

    @GetMapping("/my-orders/status/{status}")
    @Operation(summary = "Get my orders by status")
    public ResponseEntity<ApiResponse<PaginatedResponse<OrderResponse>>> getMyOrdersByStatus(
            @PathVariable OrderStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
Long userId    ) {

        Pageable pageable = PageRequest.of(page, size);
        Page<OrderResponse> orders = orderService.getUserOrdersByStatus(userId, status, pageable);
        return ResponseEntity.ok(ApiResponse.success(PaginatedResponse.from(orders)));
    }

    @PutMapping("/{id}/cancel")
    @Operation(summary = "Cancel order")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(
            @PathVariable Long id,
            @RequestParam String reason,
            Long userId
    ) {

        OrderResponse response = orderService.cancelOrder(id, reason, userId);
        return ResponseEntity.ok(ApiResponse.success("Order cancelled successfully", response));
    }

    @GetMapping("/admin/all")
//    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all orders (Admin)")
    public ResponseEntity<ApiResponse<PaginatedResponse<OrderResponse>>> getAllOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "orderDate") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        Page<OrderResponse> orders = orderService.getAllOrders(pageable);
        return ResponseEntity.ok(ApiResponse.success(PaginatedResponse.from(orders)));
    }

    @GetMapping("/admin/status/{status}")

    @Operation(summary = "Get orders by status (Admin)")
    public ResponseEntity<ApiResponse<PaginatedResponse<OrderResponse>>> getOrdersByStatus(
            @PathVariable OrderStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("orderDate").descending());
        Page<OrderResponse> orders = orderService.getOrdersByStatus(status, pageable);
        return ResponseEntity.ok(ApiResponse.success(PaginatedResponse.from(orders)));
    }

    @PutMapping("/admin/{id}")
//    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update order (Admin)")
    public ResponseEntity<ApiResponse<OrderResponse>> updateOrder(
            @PathVariable Long id,
            @Valid @RequestBody OrderUpdateRequest request
    ) {
        OrderResponse response = orderService.updateOrderStatus(id, request);
        return ResponseEntity.ok(ApiResponse.success("Order updated successfully", response));
    }

    @PutMapping("/admin/{id}/confirm")

    @Operation(summary = "Confirm order (Admin)")
    public ResponseEntity<ApiResponse<OrderResponse>> confirmOrder(@PathVariable Long id) {
        OrderResponse response = orderService.confirmOrder(id);
        return ResponseEntity.ok(ApiResponse.success("Order confirmed", response));
    }

    @PutMapping("/admin/{id}/ship")
    @Operation(summary = "Ship order (Admin)")
    public ResponseEntity<ApiResponse<OrderResponse>> shipOrder(
            @PathVariable Long id,
            @RequestParam String trackingNumber,
            @RequestParam(required = false) String carrier
    ) {
        OrderResponse response = orderService.shipOrder(id, trackingNumber, carrier);
        return ResponseEntity.ok(ApiResponse.success("Order shipped", response));
    }

    @PutMapping("/admin/{id}/deliver")
//    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Mark as delivered (Admin)")
    public ResponseEntity<ApiResponse<OrderResponse>> deliverOrder(@PathVariable Long id) {
        OrderResponse response = orderService.deliverOrder(id);
        return ResponseEntity.ok(ApiResponse.success("Order delivered", response));
    }

    @PutMapping("/admin/{id}/refund")
    @Operation(summary = "Refund order (Admin)")
    public ResponseEntity<ApiResponse<OrderResponse>> refundOrder(
            @PathVariable Long id,
            @RequestParam BigDecimal amount,
            @RequestParam String reason
    ) {
        OrderResponse response = orderService.refundOrder(id, amount, reason);
        return ResponseEntity.ok(ApiResponse.success("Order refunded", response));
    }

    @PutMapping("/admin/{id}/payment-status")
    @Operation(summary = "Update payment status (Admin)")
    public ResponseEntity<ApiResponse<OrderResponse>> updatePaymentStatus(
            @PathVariable Long id,
            @RequestParam String status
    ) {
        OrderResponse response = orderService.updatePaymentStatus(id, status);
        return ResponseEntity.ok(ApiResponse.success("Payment status updated", response));
    }

    @GetMapping("/admin/statistics")

    @Operation(summary = "Get order statistics (Admin)")
    public ResponseEntity<ApiResponse<OrderStatsResponse>> getStatistics() {
        OrderStatsResponse stats = orderService.getOrderStatistics();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }



}