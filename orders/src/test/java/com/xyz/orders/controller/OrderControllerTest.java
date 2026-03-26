package com.xyz.orders.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xyz.orders.config.SecurityConfig;
import com.xyz.orders.dto.PlaceOrderRequest;
import com.xyz.orders.exception.GlobalExceptionHandler;
import com.xyz.orders.model.Order;
import com.xyz.orders.model.OrderItem;
import com.xyz.orders.model.OrderStatus;
import com.xyz.orders.model.Product;
import com.xyz.orders.service.NotificationService;
import com.xyz.orders.service.OrderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static com.xyz.orders.controller.ControllerTestSupport.jwtWithRoles;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = OrderController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OrderService orderService;

    @MockitoBean
    private NotificationService notificationService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    @DisplayName("POST /api/orders creates order and triggers notification")
    void placeOrder() throws Exception {
        PlaceOrderRequest body = new PlaceOrderRequest(
                "Ann",
                "+100",
                "Addr",
                "buyer@test.com",
                List.of(new PlaceOrderRequest.OrderItemRequest(1L, 2))
        );
        Order order = sampleOrder(99L, "Ann", OrderStatus.PENDING, "20.00");
        when(orderService.createOrder(eq("Ann"), eq("+100"), eq("Addr"), any())).thenReturn(order);

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body))
                        .with(jwtWithRoles("sub-1", "ROLE_USER")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").value(99))
                .andExpect(jsonPath("$.customerName").value("Ann"));

        verify(notificationService, timeout(5000)).sendOrderConfirmation(
                eq("buyer@test.com"),
                eq("Ann"),
                eq(99L),
                any(BigDecimal.class));
    }

    @Test
    @DisplayName("POST /api/orders with empty items returns 400")
    void placeOrderValidation() throws Exception {
        String json = """
                {
                  "customerName": "Ann",
                  "mobileNumber": "+1",
                  "email": "a@b.com",
                  "items": []
                }
                """;

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json)
                        .with(jwtWithRoles("u", "ROLE_USER")))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/orders/{id} returns order")
    void getOrder() throws Exception {
        when(orderService.findById(3L)).thenReturn(sampleOrder(3L, "Ann", OrderStatus.CONFIRMED, "10.00"));

        mockMvc.perform(get("/api/orders/3")
                        .with(jwtWithRoles("u", "ROLE_USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(3))
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    @DisplayName("GET /api/orders lists orders for JWT subject")
    void listOrders() throws Exception {
        Order o = sampleOrder(1L, "alice", OrderStatus.PENDING, "5.00");
        when(orderService.findByUsername("alice")).thenReturn(List.of(o));

        mockMvc.perform(get("/api/orders")
                        .with(jwtWithRoles("alice", "ROLE_USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    @DisplayName("GET /api/orders?status=PENDING filters by status")
    void listOrdersWithStatus() throws Exception {
        when(orderService.findByUsernameAndStatus("alice", OrderStatus.PENDING)).thenReturn(List.of());

        mockMvc.perform(get("/api/orders")
                        .param("status", "PENDING")
                        .with(jwtWithRoles("alice", "ROLE_USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("PATCH /api/orders/{id}/status updates status")
    void updateStatus() throws Exception {
        Order updated = sampleOrder(8L, "Ann", OrderStatus.SHIPPED, "15.00");
        when(orderService.updateStatus(8L, OrderStatus.SHIPPED)).thenReturn(updated);

        mockMvc.perform(patch("/api/orders/8/status")
                        .param("status", "SHIPPED")
                        .with(jwtWithRoles("u", "ROLE_USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SHIPPED"));
    }

    @Test
    @DisplayName("POST /api/orders/{id}/cancel returns 204")
    void cancelOrder() throws Exception {
        mockMvc.perform(post("/api/orders/8/cancel")
                        .with(jwtWithRoles("u", "ROLE_USER")))
                .andExpect(status().isNoContent());

        verify(orderService).cancelOrder(8L);
    }

    @Test
    @DisplayName("unauthenticated request returns 401")
    void unauthorized() throws Exception {
        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isUnauthorized());
    }

    private static Order sampleOrder(Long id, String username, OrderStatus status, String total) {
        Product p = Product.builder()
                .id(1L)
                .name("P")
                .price(BigDecimal.TEN)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        Order order = Order.builder()
                .id(id)
                .username(username)
                .userMobile("+1")
                .status(status)
                .totalAmount(new BigDecimal(total))
                .shippingAddress("addr")
                .items(new ArrayList<>())
                .createdAt(LocalDateTime.parse("2025-01-01T12:00:00"))
                .build();
        OrderItem item = OrderItem.builder()
                .id(1L)
                .order(order)
                .product(p)
                .quantity(1)
                .price(BigDecimal.TEN)
                .build();
        order.getItems().add(item);
        return order;
    }
}
