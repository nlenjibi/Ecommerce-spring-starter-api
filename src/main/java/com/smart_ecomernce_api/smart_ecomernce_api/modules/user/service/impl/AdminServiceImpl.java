package com.smart_ecomernce_api.smart_ecomernce_api.modules.user.service.impl;

import com.smart_ecomernce_api.Smart_ecommerce_api.modules.order.entity.PaymentStatus;
import com.smart_ecomernce_api.Smart_ecommerce_api.modules.order.repository.OrderRepository;
import com.smart_ecomernce_api.Smart_ecommerce_api.modules.product.repository.ProductRepository;
import com.smart_ecomernce_api.Smart_ecommerce_api.modules.user.entity.AdminDashboardDto;
import com.smart_ecomernce_api.Smart_ecommerce_api.modules.user.repository.UserRepository;
import com.smart_ecomernce_api.Smart_ecommerce_api.modules.user.service.AdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    /**
     * Get dashboard statistics
     */
    @Cacheable(value = "admin-dashboard", key = "'global'")
    public AdminDashboardDto getDashboardStats() {
        log.info("Calculating dashboard statistics");

        return AdminDashboardDto.builder()
                .totalUsers(userRepository.count())
                .totalOrders(orderRepository.count())
                .totalProducts(productRepository.count())
                .totalRevenue(calculateTotalRevenue())
                .pendingOrders(orderRepository.countByPaymentStatus(PaymentStatus.PENDING))
                .activeUsers(userRepository.countByIsActive(true))
                .build();
    }

    /**
     * Calculate total revenue
     */

    private BigDecimal calculateTotalRevenue() {
        // Implementation depends on your Order entity structure
        return orderRepository.calculateTotalRevenue();
    }
}
