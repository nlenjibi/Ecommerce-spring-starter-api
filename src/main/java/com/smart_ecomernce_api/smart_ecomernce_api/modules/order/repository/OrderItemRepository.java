package com.smart_ecomernce_api.smart_ecomernce_api.modules.order.repository;


import com.smart_ecomernce_api.smart_ecomernce_api.modules.order.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    // Find items by order
    List<OrderItem> findByOrderId(Long orderId);

    // Find items by product
    List<OrderItem> findByProductId(Long productId);

    // Get best selling product
    @Query("SELECT oi.product.id, oi.product.name, SUM(oi.quantity) as totalSold " +
            "FROM OrderItem oi WHERE oi.order.status = 'DELIVERED' " +
            "GROUP BY oi.product.id, oi.product.name " +
            "ORDER BY totalSold DESC")
    List<Object[]> findBestSellingProducts(@Param("limit") int limit);

}
