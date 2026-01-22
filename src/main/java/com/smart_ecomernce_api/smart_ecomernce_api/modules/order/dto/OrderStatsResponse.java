package com.smart_ecomernce_api.smart_ecomernce_api.modules.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderStatsResponse {
    // Total counts
    private long totalOrders;
    private long pendingOrders;
    private long confirmedOrders;
    private long processingOrders;
    private long shippedOrders;
    private long deliveredOrders;
    private long cancelledOrders;

    // Revenue metrics
    private BigDecimal totalRevenue;
    private BigDecimal monthlyRevenue;
}
