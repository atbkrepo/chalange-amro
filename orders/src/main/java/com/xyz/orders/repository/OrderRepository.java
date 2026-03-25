package com.xyz.orders.repository;

import com.xyz.orders.dto.DailySalesAmount;
import com.xyz.orders.model.Order;
import com.xyz.orders.model.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByUsername(String username);

    List<Order> findByUsernameAndStatus(String username, OrderStatus status);

    List<Order> findByStatus(OrderStatus status);

    @Query("""
            select new com.xyz.orders.dto.DailySalesAmount(
                cast(o.createdAt as date),
                sum(o.totalAmount)
            )
            from Order o
            where o.status in :statuses
              and o.createdAt >= :rangeStart
              and o.createdAt <= :rangeEnd
            group by cast(o.createdAt as date)
            order by cast(o.createdAt as date)
            """)
    List<DailySalesAmount> sumTotalAmountPerDay(
            @Param("rangeStart") LocalDateTime rangeStart,
            @Param("rangeEnd") LocalDateTime rangeEnd,
            @Param("statuses") Set<OrderStatus> statuses);
}
