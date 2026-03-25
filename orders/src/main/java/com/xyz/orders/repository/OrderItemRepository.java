package com.xyz.orders.repository;

import com.xyz.orders.dto.ProductSalesRow;
import com.xyz.orders.model.OrderItem;
import com.xyz.orders.model.OrderStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByOrderId(Long orderId);

    @Query("""
            select new com.xyz.orders.dto.ProductSalesRow(
                p.id,
                p.name,
                sum(oi.quantity)
            )
            from OrderItem oi
            join oi.order o
            join oi.product p
            where o.status in :statuses
              and o.createdAt >= :startDay
              and o.createdAt <= :endDay
            group by p.id, p.name
            order by sum(oi.quantity) desc
            """)
    List<ProductSalesRow> findTopSellingProductsByDay(
            @Param("startDay") LocalDateTime startDay,
            @Param("endDay") LocalDateTime endDay,
            @Param("statuses") Set<OrderStatus> statuses,
            Pageable pageable);

    @Query("""
            select new com.xyz.orders.dto.ProductSalesRow(
                p.id,
                p.name,
                sum(oi.quantity)
            )
            from OrderItem oi
            join oi.order o
            join oi.product p
            where o.status in :statuses
              and o.createdAt >= :monthStart
              and o.createdAt <= :monthEnd
            group by p.id, p.name
            order by sum(oi.quantity) asc
            """)
    List<ProductSalesRow> findLeastSellingProductsByMonth(
            @Param("monthStart") LocalDateTime monthStart,
            @Param("monthEnd") LocalDateTime monthEnd,
            @Param("statuses") Set<OrderStatus> statuses,
            Pageable pageable);
}
