package com.xyz.orders.repository;

import com.xyz.orders.dto.DailySalesAmount;
import com.xyz.orders.model.Order;
import com.xyz.orders.model.OrderStatus;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Transactional
class OrderRepositoryTest {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("findByUsername returns all orders for user")
    void findByUsername() {
        orderRepository.save(order("alice", OrderStatus.PENDING, "10.00"));
        orderRepository.save(order("bob", OrderStatus.PENDING, "5.00"));
        orderRepository.save(order("alice", OrderStatus.CONFIRMED, "20.00"));

        List<Order> alice = orderRepository.findByUsername("alice");

        assertThat(alice).hasSize(2);
        assertThat(alice).extracting(Order::getUsername).containsOnly("alice");
    }

    @Test
    @DisplayName("findByUsernameAndStatus filters by both")
    void findByUsernameAndStatus() {
        orderRepository.save(order("alice", OrderStatus.PENDING, "1.00"));
        orderRepository.save(order("alice", OrderStatus.SHIPPED, "2.00"));

        List<Order> pending = orderRepository.findByUsernameAndStatus("alice", OrderStatus.PENDING);

        assertThat(pending).hasSize(1);
        assertThat(pending.getFirst().getStatus()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    @DisplayName("findByStatus returns matching orders")
    void findByStatus() {
        orderRepository.save(order("u1", OrderStatus.CANCELLED, "1.00"));
        orderRepository.save(order("u2", OrderStatus.CANCELLED, "2.00"));
        orderRepository.save(order("u3", OrderStatus.PENDING, "3.00"));

        List<Order> cancelled = orderRepository.findByStatus(OrderStatus.CANCELLED);

        assertThat(cancelled).hasSize(2);
    }

    @Test
    @DisplayName("sumTotalAmountPerDay aggregates by calendar day for statuses in range")
    void sumTotalAmountPerDay() {
        Order a = orderRepository.save(order("store", OrderStatus.CONFIRMED, "100.00"));
        Order b = orderRepository.save(order("store", OrderStatus.CONFIRMED, "50.00"));
        Order c = orderRepository.save(order("store", OrderStatus.SHIPPED, "25.00"));
        Order excludedStatus = orderRepository.save(order("store", OrderStatus.PENDING, "999.00"));
        entityManager.flush();

        setCreatedAt(a.getId(), LocalDateTime.of(2025, 6, 1, 10, 0));
        setCreatedAt(b.getId(), LocalDateTime.of(2025, 6, 1, 18, 30));
        setCreatedAt(c.getId(), LocalDateTime.of(2025, 6, 2, 9, 0));
        setCreatedAt(excludedStatus.getId(), LocalDateTime.of(2025, 6, 1, 12, 0));
        entityManager.flush();
        entityManager.clear();

        LocalDateTime start = LocalDateTime.of(2025, 6, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2025, 6, 2, 23, 59, 59);
        Set<OrderStatus> statuses = EnumSet.of(OrderStatus.CONFIRMED, OrderStatus.SHIPPED);

        List<DailySalesAmount> rows = orderRepository.sumTotalAmountPerDay(start, end, statuses);

        assertThat(rows).hasSize(2);
        DailySalesAmount day1 = rows.stream()
                .filter(r -> r.day().equals(LocalDate.of(2025, 6, 1)))
                .findFirst()
                .orElseThrow();
        assertThat(day1.totalAmount()).isEqualByComparingTo("150.00");

        DailySalesAmount day2 = rows.stream()
                .filter(r -> r.day().equals(LocalDate.of(2025, 6, 2)))
                .findFirst()
                .orElseThrow();
        assertThat(day2.totalAmount()).isEqualByComparingTo("25.00");
    }

    private void setCreatedAt(Long orderId, LocalDateTime createdAt) {
        entityManager.createNativeQuery(
                        "UPDATE orders SET created_at = ?, updated_at = ? WHERE id = ?")
                .setParameter(1, createdAt)
                .setParameter(2, createdAt)
                .setParameter(3, orderId)
                .executeUpdate();
    }

    private static Order order(String username, OrderStatus status, String total) {
        return Order.builder()
                .username(username)
                .userMobile("+100000")
                .status(status)
                .totalAmount(new BigDecimal(total))
                .shippingAddress("addr")
                .build();
    }
}
