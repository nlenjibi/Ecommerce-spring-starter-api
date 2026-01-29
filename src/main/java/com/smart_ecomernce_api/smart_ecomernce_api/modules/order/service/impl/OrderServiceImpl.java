package com.smart_ecomernce_api.smart_ecomernce_api.modules.order.service.impl;

import com.smart_ecomernce_api.smart_ecomernce_api.exception.InsufficientStockException;
import com.smart_ecomernce_api.smart_ecomernce_api.exception.ResourceNotFoundException;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.order.dto.*;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.order.entity.Order;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.order.entity.OrderItem;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.order.entity.OrderStatus;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.order.entity.PaymentStatus;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.order.mapper.OrderMapper;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.order.repository.OrderRepository;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.order.service.OrderService;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.product.entity.Product;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.product.repository.ProductRepository;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.user.entity.User;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;


@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderMapper orderMapper;

    @Override
    @CacheEvict(value = "orders", allEntries = true)
    public OrderResponse createOrder(OrderCreateRequest request, Long userId) {
        log.info("Creating order for user {}", userId);

        // Validate user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ResourceNotFoundException.forResource("User", userId));

        // Create order builder
        Order order = Order.builder()
                .user(user)
                .customerEmail(user.getEmail())
                .customerName(user.getFirstName() + " " + user.getLastName())
                .status(OrderStatus.PENDING)
                .paymentStatus(PaymentStatus.PENDING)
                .build();

        // Add order items
        for (OrderItemRequest itemRequest : request.getItems()) {
            Product product = productRepository.findById(itemRequest.getProductId())
                    .orElseThrow(() -> ResourceNotFoundException.forResource("Product", itemRequest.getProductId()));

            if (!product.getIsActive()) {
                throw new ResourceNotFoundException("Product is not available");
            }

            // Validate stock
            if (!product.canBeOrdered(itemRequest.getQuantity())) {
                throw new InsufficientStockException(
                        product.getName(),
                        product.getAvailableQuantity(),
                        itemRequest.getQuantity()
                );
            }

            // Reserve stock
            product.reserveStock(itemRequest.getQuantity());

            // Create order item
            OrderItem orderItem = OrderItem.builder()
                    .product(product)
                    .productName(product.getName())
                    .quantity(itemRequest.getQuantity())
                    .unitPrice(product.getEffectivePrice())
                    .build();

            order.addOrderItem(orderItem);
        }

        // Apply business rules
        if (request.getTaxRate() != null) {
            order.applyTax(request.getTaxRate());
        }


        if (request.getCouponCode() != null && request.getCouponDiscount() != null) {
            order.applyCoupon(request.getCouponCode(), request.getCouponDiscount());
        }

        // Calculate totals
        order.calculateTotals();

        // Save order
        Order savedOrder = orderRepository.save(order);

        log.info("Order created successfully: {} with total: ${}",
                savedOrder.getOrderNumber(),
                savedOrder.getTotalAmount()
        );

        return orderMapper.toDto(savedOrder);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "orders", key = "#id + '_' + #userId")
    public OrderResponse getOrderById(Long id, Long userId) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.forResource("Order", id));

        // Verify ownership
        if (!order.isPlacedBy(userRepository.findById(userId).orElseThrow())) {
            throw new ResourceNotFoundException("You don't have access to this order");
        }

        return orderMapper.toDto(order);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "orders", key = "#orderNumber + '_' + #userId")
    public OrderResponse getOrderByOrderNumber(String orderNumber, Long userId) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> ResourceNotFoundException.forResource("Order not found: ",orderNumber));

        // Verify ownership
        if (!order.isPlacedBy(userRepository.findById(userId).orElseThrow())) {
            throw ResourceNotFoundException.forResource("You don't have access to this order", orderNumber);
        }

        return orderMapper.toDto(order);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "orders", key = "'user_' + #userId + '_' + #pageable.pageNumber + '_' + #pageable.pageSize")
    public Page<OrderResponse> getUserOrders(Long userId, Pageable pageable) {
        return orderRepository.findByUserIdOrderByOrderDateDesc(userId, pageable)
                .map(orderMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "orders", key = "'user_status_' + #userId + '_' + #status + '_' + #pageable.pageNumber + '_' + #pageable.pageSize")
    public Page<OrderResponse> getUserOrdersByStatus(
            Long userId,
            OrderStatus status,
            Pageable pageable
    ) {
        return orderRepository.findByUserIdAndStatusOrderByOrderDateDesc(userId, status, pageable)
                .map(orderMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "orders", key = "'all_' + #pageable.pageNumber + '_' + #pageable.pageSize")
    public Page<OrderResponse> getAllOrders(Pageable pageable) {
        return orderRepository.findAll(pageable)
                .map(orderMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "orders", key = "'status_' + #status + '_' + #pageable.pageNumber + '_' + #pageable.pageSize")
    public Page<OrderResponse> getOrdersByStatus(OrderStatus status, Pageable pageable) {
        return orderRepository.findByStatusOrderByOrderDateDesc(status, pageable)
                .map(orderMapper::toDto);
    }

    @Override
    public OrderResponse updateOrderStatus(Long id, OrderUpdateRequest request) {
        // Invalidate order caches since status changes
        // Cache eviction will be performed by the caller flow via annotations when appropriate
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.forResource("Order", id));

        if (request.getStatus() != null) {
            updateStatus(order, request.getStatus());
        }

        if (request.getTrackingNumber() != null) {
            order.setPaymentTransactionId(request.getTrackingNumber());
        }

        Order updated = orderRepository.save(order);
        return orderMapper.toDto(updated);
    }

    @Override
    public OrderResponse updatePaymentStatus(Long orderId, String statusStr) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> ResourceNotFoundException.forResource("user", orderId));

        try {
            PaymentStatus status = PaymentStatus.valueOf(statusStr.toUpperCase());
            order.setPaymentStatus(status);

            if (status == PaymentStatus.PAID) {
                order.markAsPaid(generateTransactionId());
            }

            Order updated = orderRepository.save(order);
            log.info("Payment status updated for order {}: {}", orderId, status);
            return orderMapper.toDto(updated);

        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid payment status: " + statusStr);
        }
    }

    @Override
    @CacheEvict(value = "orders", allEntries = true)
    public OrderResponse confirmOrder(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.forResource("Order", id));

        order.confirm();
        Order confirmed = orderRepository.save(order);

        log.info("Order confirmed: {}", order.getOrderNumber());
        return orderMapper.toDto(confirmed);
    }

    @Override
    @CacheEvict(value = "orders", allEntries = true)
    public OrderResponse shipOrder(Long id, String trackingNumber, String carrier) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.forResource("user", id));

        // Deduct stock when shipped (complete the reservation)
        for (OrderItem item : order.getOrderItems()) {
            item.getProduct().deductStock(item.getQuantity());
        }

        order.ship();
        order.setPaymentTransactionId(trackingNumber);

        Order shipped = orderRepository.save(order);

        log.info("Order shipped: {} with tracking: {}", order.getOrderNumber(), trackingNumber);
        return orderMapper.toDto(shipped);
    }

    @Override
    @CacheEvict(value = "orders", allEntries = true)
    public OrderResponse deliverOrder(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.forResource("user", id));


        order.deliver();
        Order delivered = orderRepository.save(order);

        log.info("Order delivered: {}", order.getOrderNumber());
        return orderMapper.toDto(delivered);
    }

    @Override
    @CacheEvict(value = "orders", allEntries = true)
    public OrderResponse cancelOrder(Long id, String reason, Long userId) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.forResource("user", id));

        // Verify ownership
        if (!order.isPlacedBy(userRepository.findById(userId).orElseThrow())) {
            throw new ResourceNotFoundException("You can only cancel your own orders");
        }

        if (!order.canBeCancelled()) {
            throw new IllegalStateException("Order cannot be cancelled at this stage");
        }

        // Release reserved stock
        for (OrderItem item : order.getOrderItems()) {
            item.getProduct().releaseReservedStock(item.getQuantity());
        }

        order.cancel(reason);
        Order cancelled = orderRepository.save(order);

        log.info("Order cancelled: {} - Reason: {}", order.getOrderNumber(), reason);
        return orderMapper.toDto(cancelled);
    }

    @Override
    @CacheEvict(value = "orders", allEntries = true)
    public OrderResponse refundOrder(Long id, BigDecimal amount, String reason) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.forResource("user", id));

        if (!order.canBeRefunded()) {
            throw new IllegalStateException("Order cannot be refunded");
        }

        order.refund(amount, reason);
        Order refunded = orderRepository.save(order);

        log.info("Order refunded: {} - Amount: ${}", order.getOrderNumber(), amount);
        return orderMapper.toDto(refunded);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "orders", key = "'stats'")
    public OrderStatsResponse getOrderStatistics() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime monthStart = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);

        return OrderStatsResponse.builder()
                .totalOrders(orderRepository.count())
                .pendingOrders(orderRepository.countByStatus(OrderStatus.PENDING))
                .confirmedOrders(orderRepository.countByStatus(OrderStatus.CONFIRMED))
                .processingOrders(orderRepository.countByStatus(OrderStatus.PROCESSING))
                .shippedOrders(orderRepository.countByStatus(OrderStatus.SHIPPED))
                .deliveredOrders(orderRepository.countByStatus(OrderStatus.DELIVERED))
                .cancelledOrders(orderRepository.countByStatus(OrderStatus.CANCELLED))
                .totalRevenue(calculateRevenue())
                .monthlyRevenue(orderRepository.calculateRevenue(monthStart, now))
                .build();
    }

    // Helper methods
    private void updateStatus(Order order, OrderStatus newStatus) {
        switch (newStatus) {
            case CONFIRMED -> order.confirm();
            case PROCESSING -> order.process();
            case SHIPPED -> order.ship();
            case OUT_FOR_DELIVERY -> order.outForDelivery();
            case DELIVERED -> order.deliver();
            default -> order.setStatus(newStatus);
        }
    }

    private BigDecimal calculateRevenue() {
        BigDecimal revenue = orderRepository.calculateTotalRevenue();
        return revenue != null ? revenue : BigDecimal.ZERO;
    }

    private String generateTransactionId() {
        return "TXN-" + System.currentTimeMillis() + "-" +
                java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
