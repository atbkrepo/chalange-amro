package com.xyz.orders.repository;

import com.xyz.orders.model.Order;
import com.xyz.orders.model.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByUsername(String username);

    List<Order> findByUsernameAndStatus(String username, OrderStatus status);

    List<Order> findByStatus(OrderStatus status);
}
