package com.xyz.orders.service;

import com.xyz.orders.exception.ResourceNotFoundException;
import com.xyz.orders.model.Order;
import com.xyz.orders.model.OrderItem;
import com.xyz.orders.model.OrderStatus;
import com.xyz.orders.model.Product;
import com.xyz.orders.repository.OrderRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductService productService;

    @Mock
    private InventoryService inventoryService;

    @InjectMocks
    private OrderService orderService;

    @Test
    @DisplayName("findById throws when order does not exist")
    void findByIdMissing() {
        when(orderRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.findById(1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Order");
    }

    @Test
    @DisplayName("createOrder reserves stock, computes total, saves order")
    void createOrder() {
        Product p1 = Product.builder().id(1L).price(new BigDecimal("2.50")).build();
        when(productService.findById(1L)).thenReturn(p1);
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> {
            Order o = i.getArgument(0);
            if (o.getId() == null) {
                o.setId(100L);
            }
            return o;
        });

        Order result = orderService.createOrder(
                "user1",
                "+48100000",
                "Addr",
                List.of(new OrderService.OrderItemRequest(1L, 3))
        );

        assertThat(result.getTotalAmount()).isEqualByComparingTo("7.50");
        assertThat(result.getUsername()).isEqualTo("user1");
        verify(inventoryService).reserveStock(1L, 3);
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    @DisplayName("cancelOrder only allowed from PENDING")
    void cancelOrderNotPending() {
        Order order = Order.builder()
                .id(5L)
                .status(OrderStatus.CONFIRMED)
                .username("user")
                .userMobile("mob")
                .items(new ArrayList<>())
                .build();
        when(orderRepository.findById(5L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancelOrder(5L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("PENDING");
    }

    @Test
    @DisplayName("cancelOrder from PENDING moves to CANCELLED and releases stock")
    void cancelOrderPending() {
        Product product = Product.builder().id(7L).build();
        OrderItem item = OrderItem.builder().product(product).quantity(2).build();
        Order order = Order.builder()
                .id(5L)
                .status(OrderStatus.PENDING)
                .username("user")
                .userMobile("mob")
                .items(new ArrayList<>(List.of(item)))
                .build();
        when(orderRepository.findById(5L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        orderService.cancelOrder(5L);

        verify(inventoryService).releaseStock(7L, 2);
        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    @DisplayName("advanceStoreOrderStatus rejects invalid target status")
    void advanceStoreInvalidTarget() {
        assertThatThrownBy(() -> orderService.advanceStoreOrderStatus(1L, OrderStatus.DELIVERED))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("CONFIRMED");
    }

    @Test
    @DisplayName("advanceStoreOrderStatus rejects when order is cancelled")
    void advanceStoreCancelledOrder() {
        Order order = Order.builder()
                .id(1L)
                .status(OrderStatus.CANCELLED)
                .username("user")
                .userMobile("mob")
                .items(new ArrayList<>())
                .build();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.advanceStoreOrderStatus(1L, OrderStatus.CONFIRMED))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("cancelled");
    }

    @Test
    @DisplayName("updateStatus to CONFIRMED confirms stock deduction per line")
    void updateStatusConfirmed() {
        Product product = Product.builder().id(3L).build();
        OrderItem item = OrderItem.builder().product(product).quantity(4).build();
        Order order = Order.builder()
                .id(9L)
                .status(OrderStatus.PENDING)
                .username("user")
                .userMobile("mob")
                .items(new ArrayList<>(List.of(item)))
                .build();
        when(orderRepository.findById(9L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        orderService.updateStatus(9L, OrderStatus.CONFIRMED);

        verify(inventoryService).confirmStockDeduction(3L, 4);
        verify(inventoryService, never()).releaseStock(anyLong(), anyInt());
    }

    @Test
    @DisplayName("findAllForStore with id returns single order or empty list")
    void findAllForStoreById() {
        Order o = Order.builder().id(2L).username("user").userMobile("mob").build();
        when(orderRepository.findById(2L)).thenReturn(Optional.of(o));

        assertThat(orderService.findAllForStore(2L)).containsExactly(o);
    }

    @Test
    @DisplayName("findAllForStore without id returns all sorted by createdAt desc")
    void findAllForStoreAll() {
        orderService.findAllForStore(null);

        verify(orderRepository).findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
    }
}
