package com.xyz.orders.controller;

import com.xyz.orders.dto.OrderResponse;
import com.xyz.orders.dto.PlaceOrderRequest;
import com.xyz.orders.model.Order;
import com.xyz.orders.model.OrderStatus;
import com.xyz.orders.service.NotificationService;
import com.xyz.orders.service.OrderService;
import com.xyz.orders.service.OrderService.OrderItemRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Order placement and management (requires USER or ADMIN roles)")
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_USER')")
public class OrderController {

    private final OrderService orderService;
    private final NotificationService notificationService;

    @PostMapping
    @Operation(summary = "Place a new order",
            description = "Creates an order with multiple products. Customer name and mobile number are mandatory. Returns the assigned Order ID.")
    public ResponseEntity<OrderResponse> placeOrder(@AuthenticationPrincipal Jwt jwt,
                                                    @Valid @RequestBody PlaceOrderRequest request) {

        List<OrderItemRequest> items = request.items().stream()
                .map(i -> new OrderItemRequest(i.productId(), i.quantity()))
                .toList();

        Order order = this.orderService.createOrder(
                request.customerName(),
                request.mobileNumber(),
                request.shippingAddress(),
                items
        );

        OrderResponse response = OrderResponse.from(order);
        
        try (var executor = Executors.newSingleThreadExecutor()) {
            executor.submit(() ->
                    this.notificationService.sendOrderConfirmation(
                            request.email(),
                            request.customerName(),
                            order.getId(),
                            order.getTotalAmount()
                    )
            );
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{orderId}")
    @Operation(summary = "Get order details by ID")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable Long orderId) {
        Order order = this.orderService.findById(orderId);
        return ResponseEntity.ok(OrderResponse.from(order));
    }

    @GetMapping
    @Operation(summary = "List orders for the authenticated user")
    public ResponseEntity<List<OrderResponse>> listOrders(@AuthenticationPrincipal Jwt jwt,
                                                          @RequestParam(required = false) OrderStatus status) {
        String username = jwt.getSubject();
        List<Order> orders = Optional.ofNullable(status)
                .map(orderStatus -> this.orderService.findByUsernameAndStatus(username, orderStatus))
                .orElseGet(() -> this.orderService.findByUsername(username));

        return ResponseEntity.ok(orders.stream().map(OrderResponse::from).toList());
    }

    @PatchMapping("/{orderId}/status")
    @Operation(summary = "Update order status")
    public ResponseEntity<OrderResponse> updateStatus(@PathVariable Long orderId,
                                                      @RequestParam OrderStatus status) {
        Order order = this.orderService.updateStatus(orderId, status);
        return ResponseEntity.ok(OrderResponse.from(order));
    }

    @PostMapping("/{orderId}/cancel")
    @Operation(summary = "Cancel an order")
    public ResponseEntity<Void> cancelOrder(@PathVariable Long orderId) {
        this.orderService.cancelOrder(orderId);
        return ResponseEntity.noContent().build();
    }
}
