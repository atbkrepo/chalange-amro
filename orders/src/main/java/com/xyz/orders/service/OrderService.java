package com.xyz.orders.service;

import com.xyz.orders.exception.ResourceNotFoundException;
import com.xyz.orders.model.*;
import com.xyz.orders.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductService productService;
    private final InventoryService inventoryService;

    public Order findById(Long id) {
        return this.orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", id));
    }

    public List<Order> findByUsername(String username) {
        return this.orderRepository.findByUsername(username);
    }

    public List<Order> findByUsernameAndStatus(String username, OrderStatus status) {
        return this.orderRepository.findByUsernameAndStatus(username, status);
    }

    public List<Order> findByStatus(OrderStatus status) {
        return this.orderRepository.findByStatus(status);
    }

    @Transactional
    public Order createOrder(String username, String userMobile, String shippingAddress, List<OrderItemRequest> itemRequests) {
        Order order = Order.builder()
                .username(username)//not null
                .userMobile(userMobile)//not null
                .shippingAddress(shippingAddress)
                .build();

        BigDecimal totalPrice = BigDecimal.ZERO;

        for (OrderItemRequest req : itemRequests) {
            Product product = this.productService.findById(req.productId());
            this.inventoryService.reserveStock(product.getId(), req.quantity());

            OrderItem item = OrderItem.builder()
                    .order(order)
                    .product(product)
                    .quantity(req.quantity())
                    .price(product.getPrice())
                    .build();
            order.getItems().add(item);

            totalPrice = totalPrice.add(product.getPrice().multiply(BigDecimal.valueOf(req.quantity())));
        }

        order.setTotalAmount(totalPrice);
        return this.orderRepository.save(order);
    }

    @Transactional
    public Order updateStatus(Long orderId, OrderStatus newStatus) {
        Order order = this.findById(orderId);
        OrderStatus oldStatus = order.getStatus();
        order.setStatus(newStatus);

        if (newStatus == OrderStatus.CONFIRMED) {
            for (OrderItem item : order.getItems()) {
                this.inventoryService.confirmStockDeduction(item.getProduct().getId(), item.getQuantity());
            }
        } else if (newStatus == OrderStatus.CANCELLED && oldStatus == OrderStatus.PENDING) {
            for (OrderItem item : order.getItems()) {
                this.inventoryService.releaseStock(item.getProduct().getId(), item.getQuantity());
            }
        }

        return this.orderRepository.save(order);
    }

    @Transactional
    public void cancelOrder(Long orderId) {
        Order order = this.findById(orderId);
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalArgumentException("Only orders in PENDING status can be cancelled");
        }
        this.updateStatus(orderId, OrderStatus.CANCELLED);
    }

    public record OrderItemRequest(Long productId, int quantity) {}
}
