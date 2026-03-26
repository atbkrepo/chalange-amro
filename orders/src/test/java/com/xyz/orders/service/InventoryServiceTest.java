package com.xyz.orders.service;

import com.xyz.orders.exception.InsufficientStockException;
import com.xyz.orders.exception.ResourceNotFoundException;
import com.xyz.orders.model.Inventory;
import com.xyz.orders.model.Product;
import com.xyz.orders.repository.InventoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private ProductService productService;

    @InjectMocks
    private InventoryService inventoryService;

    @Test
    @DisplayName("reserveStock increases reserved quantity when stock available")
    void reserveStockSuccess() {
        Product product = Product.builder().id(10L).build();
        Inventory inv = Inventory.builder()
                .id(1L)
                .product(product)
                .quantity(100)
                .reservedQuantity(5)
                .build();
        when(inventoryRepository.findByProductId(10L)).thenReturn(Optional.of(inv));
        when(inventoryRepository.save(any(Inventory.class))).thenAnswer(i -> i.getArgument(0));

        Inventory result = inventoryService.reserveStock(10L, 10);

        assertThat(result.getReservedQuantity()).isEqualTo(15);
        verify(inventoryRepository).save(inv);
    }

    @Test
    @DisplayName("reserveStock throws when not enough available quantity")
    void reserveStockInsufficient() {
        Product product = Product.builder().id(10L).build();
        Inventory inv = Inventory.builder()
                .product(product)
                .quantity(10)
                .reservedQuantity(8)
                .build();
        when(inventoryRepository.findByProductId(10L)).thenReturn(Optional.of(inv));

        assertThatThrownBy(() -> inventoryService.reserveStock(10L, 5))
                .isInstanceOf(InsufficientStockException.class);
    }

    @Test
    @DisplayName("findByProductId throws ResourceNotFoundException when missing")
    void findByProductIdMissing() {
        when(inventoryRepository.findByProductId(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> inventoryService.findByProductId(1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Inventory for product");
    }

    @Test
    @DisplayName("initializeStock creates row for product")
    void initializeStock() {
        Product p = Product.builder().id(2L).build();
        when(productService.findById(2L)).thenReturn(p);
        when(inventoryRepository.save(any(Inventory.class))).thenAnswer(i -> i.getArgument(0));

        Inventory inv = inventoryService.initializeStock(2L, 50);

        assertThat(inv.getQuantity()).isEqualTo(50);
        assertThat(inv.getReservedQuantity()).isZero();
        assertThat(inv.getProduct()).isSameAs(p);
    }
}
