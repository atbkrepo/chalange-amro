package com.xyz.orders.service;

import com.xyz.orders.exception.ResourceNotFoundException;
import com.xyz.orders.model.Product;
import com.xyz.orders.repository.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    @Test
    @DisplayName("findById returns product when present")
    void findByIdFound() {
        Product p = Product.builder().id(1L).name("Book").price(BigDecimal.TEN).build();
        when(productRepository.findById(1L)).thenReturn(Optional.of(p));

        assertThat(productService.findById(1L)).isSameAs(p);
    }

    @Test
    @DisplayName("findById throws when missing")
    void findByIdMissing() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Product")
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("findAll delegates to repository")
    void findAll() {
        List<Product> list = List.of(Product.builder().name("A").price(BigDecimal.ONE).build());
        when(productRepository.findAll()).thenReturn(list);

        assertThat(productService.findAll()).isEqualTo(list);
    }

    @Test
    @DisplayName("update copies fields and saves")
    void update() {
        Product existing = Product.builder()
                .id(1L)
                .name("Old")
                .description("d")
                .price(BigDecimal.ONE)
                .build();
        Product incoming = Product.builder()
                .name("New")
                .description("nd")
                .price(new BigDecimal("9.99"))
                .build();
        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        Product result = productService.update(1L, incoming);

        assertThat(result.getName()).isEqualTo("New");
        assertThat(result.getDescription()).isEqualTo("nd");
        assertThat(result.getPrice()).isEqualByComparingTo("9.99");
        verify(productRepository).save(existing);
    }

    @Test
    @DisplayName("delete loads then removes product")
    void delete() {
        Product p = Product.builder().id(3L).name("X").price(BigDecimal.ONE).build();
        when(productRepository.findById(3L)).thenReturn(Optional.of(p));

        productService.delete(3L);

        verify(productRepository).delete(p);
    }
}
