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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;


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
    public Page<OrderResponse> getUserOrders(Long userId, Pageable pageable) {
        List<Order> orders = orderRepository.findByUserId(userId, pageable.getPageNumber(), pageable.getPageSize());
        long total = orderRepository.countByUserId(userId);
        return new PageImpl<>(orders.stream().map(orderMapper::toDto).collect(Collectors.toList()), pageable, total);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> getUserOrdersByStatus(
            Long userId,
            OrderStatus status,
            Pageable pageable
    ) {
        List<Order> orders = orderRepository.findByUserIdAndStatus(userId, status, pageable.getPageNumber(), pageable.getPageSize());
        long total = orderRepository.countByUserId(userId); // Note: ideally should be count by user and status, but method not available
        return new PageImpl<>(orders.stream().map(orderMapper::toDto).collect(Collectors.toList()), pageable, total);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> getAllOrders(Pageable pageable) {
        List<Order> orders = orderRepository.findAll(pageable.getPageNumber(), pageable.getPageSize());
        long total = orderRepository.count();
        return new PageImpl<>(orders.stream().map(orderMapper::toDto).collect(Collectors.toList()), pageable, total);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> getOrdersByStatus(OrderStatus status, Pageable pageable) {
        List<Order> orders = orderRepository.findByStatus(status, pageable.getPageNumber(), pageable.getPageSize());
        long total = orderRepository.countByStatus(status);
        return new PageImpl<>(orders.stream().map(orderMapper::toDto).collect(Collectors.toList()), pageable, total);
    }

    @Override
    @Transactional
    @CacheEvict(value = "orders", key = "#id")
    public OrderResponse updateOrderStatus(Long id, OrderUpdateRequest request) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.forResource("Order", id));

        if (request.getStatus() != null && order.getStatus() != request.getStatus()) {
            orderRepository.updateStatus(id, request.getStatus());
            log.info("Order {} status updated to {}", order.getOrderNumber(), request.getStatus());
        }

        if (request.getTrackingNumber() != null) {
            order.setPaymentTransactionId(request.getTrackingNumber());
            orderRepository.update(order);
        }

        return orderRepository.findById(id).map(orderMapper::toDto)
                .orElseThrow(() -> ResourceNotFoundException.forResource("Order", id));
    }

    @Override
    @Transactional
    @CacheEvict(value = "orders", key = "#orderId")
    public OrderResponse updatePaymentStatus(Long orderId, String statusStr) {
        orderRepository.findById(orderId)
                .orElseThrow(() -> ResourceNotFoundException.forResource("Order", orderId));

        try {
            PaymentStatus status = PaymentStatus.valueOf(statusStr.toUpperCase());
            orderRepository.updatePaymentStatus(orderId, status);

            if (status == PaymentStatus.PAID) {
                Order order = orderRepository.findById(orderId).get();
                order.markAsPaid(generateTransactionId());
                orderRepository.update(order);
            }

            log.info("Payment status updated for order {}: {}", orderId, status);
            return orderRepository.findById(orderId).map(orderMapper::toDto)
                    .orElseThrow(() -> ResourceNotFoundException.forResource("Order", orderId));

        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid payment status: " + statusStr);
        }
    }

    @Override
    @Transactional
    public OrderResponse confirmOrder(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.forResource("Order", id));

        order.confirm();
        Order confirmed = orderRepository.save(order);

        log.info("Order confirmed: {}", order.getOrderNumber());
        return orderMapper.toDto(confirmed);
    }

    @Override
    @Transactional
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
    @Transactional
    public OrderResponse deliverOrder(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.forResource("user", id));


        order.deliver();
        Order delivered = orderRepository.save(order);

        log.info("Order delivered: {}", order.getOrderNumber());
        return orderMapper.toDto(delivered);
    }

    @Override
    @Transactional
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
    @Transactional
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
    public OrderStatsResponse getOrderStatistics() {
        return orderRepository.getOrderStatistics();
    }

    @Override
    public void deleteOrder(Long orderId) {
        log.info("Deleting order with ID: {}", orderId);
        orderRepository.findById(orderId)
                .orElseThrow(() -> ResourceNotFoundException.forResource("Order", orderId));
        orderRepository.deleteById(orderId);
        log.info("Order with ID: {} deleted successfully", orderId);
    }

    @Override
    public OrderResponse getOrderByIdAsAdmin(Long id) {
        Order order = findOrderById(id);
        return orderMapper.toDto(order);
    }

    @Override
    public OrderResponse updateOrderAsCustomer(Long id, OrderUpdateRequest request, Long userId) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.forResource("Order", id));

        // Check if order is PENDING
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalStateException("Order can only be updated when status is PENDING");
        }

        // Check if the order belongs to the provided userId
        if (!order.getUser().getId().equals(userId)) {
            throw new SecurityException("You are not authorized to update this order");
        }

        // Update allowed fields (example: status, trackingNumber, carrier, adminNotes)
        if (request.getStatus() != null) {
            order.setStatus(request.getStatus());
        }
        if (request.getTrackingNumber() != null) {
            order.setPaymentTransactionId(request.getTrackingNumber());
        }
        if (request.getCarrier() != null) {
            order.setCarrier(request.getCarrier());
        }

        // Add more allowed fields as needed

        orderRepository.update(order);
        log.info("Customer updated order {} while status is PENDING", order.getOrderNumber());
        return orderMapper.toDto(order);
    }


    private Order findOrderById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.forResource("Order", id));
    }

    private void updateStatus(Order order, OrderStatus newStatus) {
        if (order.getStatus() != newStatus) {
            order.setStatus(newStatus);
            log.info("Order {} status updated to {}", order.getOrderNumber(), newStatus);
        }
    }

    private String generateTransactionId() {
        return "txn_" + System.currentTimeMillis();
    }
}
