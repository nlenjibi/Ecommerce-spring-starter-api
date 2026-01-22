package com.smart_ecomernce_api.smart_ecomernce_api.modules.cart.repository;

import com.smart_ecomernce_api.Smart_ecommerce_api.modules.cart.entity.Cart;
import com.smart_ecomernce_api.Smart_ecommerce_api.modules.cart.entity.CartStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CartRepository extends JpaRepository<Cart, UUID> {

    @Query("SELECT c FROM Cart c LEFT JOIN FETCH c.items WHERE c.id = :id")
    Optional<Cart> findByIdWithItems(@Param("id") UUID id);

    Optional<Cart> findBySessionId(String sessionId);

    @Query("SELECT c FROM Cart c LEFT JOIN FETCH c.items WHERE c.sessionId = :sessionId")
    Optional<Cart> findBySessionIdWithItems(@Param("sessionId") String sessionId);

    @Query("SELECT c FROM Cart c WHERE c.status = 'ABANDONED' AND c.dateCreated < :cutoffDate")
    List<Cart> findAbandonedCartsBefore(@Param("cutoffDate") LocalDateTime cutoffDate);

    @Query("SELECT c FROM Cart c WHERE c.status = :status")
    List<Cart> findByStatus(@Param("status") CartStatus status);

    @Query("SELECT c FROM Cart c WHERE c.status = 'ACTIVE' AND c.dateCreated < :cutoffDate " +
            "AND SIZE(c.items) = 0")  // FIXED: Use SIZE instead of EXISTS
    List<Cart> findEmptyCartsBefore(@Param("cutoffDate") LocalDateTime cutoffDate);

    @Query("SELECT c FROM Cart c WHERE c.dateCreated BETWEEN :startDate AND :endDate")
    List<Cart> findCartsCreatedBetween(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @Query("SELECT COUNT(c) FROM Cart c WHERE c.status = 'ACTIVE'")
    long countActiveCarts();

    @Query("SELECT COUNT(c) FROM Cart c WHERE c.status = 'ABANDONED'")
    long countAbandonedCarts();

    @Query("SELECT DISTINCT c FROM Cart c JOIN c.items ci WHERE ci.product.id = :productId")
    List<Cart> findCartsContainingProduct(@Param("productId") Long productId);

    @Query("SELECT c FROM Cart c JOIN c.items ci " +
            "GROUP BY c HAVING SUM(ci.quantity * ci.unitPrice) >= :minValue")
    List<Cart> findCartsWithValueAbove(@Param("minValue") Double minValue);

    @Modifying  // FIXED: Added @Modifying for DELETE query
    @Query("DELETE FROM Cart c WHERE c.status = :status AND c.dateCreated < :cutoffDate")
    int deleteByStatusAndCreatedBefore(
            @Param("status") CartStatus status,
            @Param("cutoffDate") LocalDateTime cutoffDate
    );

    @Modifying  // FIXED: Added @Modifying for UPDATE query
    @Query("UPDATE Cart c SET c.status = :newStatus WHERE c.status = :oldStatus " +
            "AND c.updatedAt < :cutoffDate")
    int updateStatusForCartsUpdatedBefore(
            @Param("oldStatus") CartStatus oldStatus,
            @Param("newStatus") CartStatus newStatus,
            @Param("cutoffDate") LocalDateTime cutoffDate
    );
}
