package com.xyz.orders.controller;

import com.xyz.orders.dto.OrderResponse;
import com.xyz.orders.model.Order;
import com.xyz.orders.model.OrderStatus;
import com.xyz.orders.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/store/orders")
@RequiredArgsConstructor
@Tag(name = "Store orders", description = "List and update order fulfilment (requires ROLE_STOCK or ROLE_ADMIN)")
@PreAuthorize("hasAnyAuthority('ROLE_STOCK','ROLE_ADMIN')")
public class StoreOrderController {

    private final OrderService orderService;

    @GetMapping
    @Operation(summary = "List all orders",
            description = "Returns every order, newest first. Pass orderId to narrow to a single order (empty list if not found).")
    public ResponseEntity<List<OrderResponse>> listOrders(@RequestParam(required = false) Long orderId) {
        List<Order> orders = this.orderService.findAllForStore(orderId);
        return ResponseEntity.ok(orders.stream().map(OrderResponse::from).toList());
    }

    @PatchMapping("/{orderId}/status")
    @Operation(summary = "Advance order fulfilment status",
            description = "Allowed: CONFIRMED,SHIPPED. Inventory is confirmed when leaving PENDING.")
    public ResponseEntity<OrderResponse> advanceStatus(@PathVariable Long orderId, @RequestParam OrderStatus status) {
        Order order = this.orderService.advanceStoreOrderStatus(orderId, status);
        return ResponseEntity.ok(OrderResponse.from(order));
    }
}
