package com.xyz.orders.controller;

import com.xyz.orders.config.SecurityConfig;
import com.xyz.orders.exception.GlobalExceptionHandler;
import com.xyz.orders.model.Product;
import com.xyz.orders.service.ProductService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static com.xyz.orders.controller.ControllerTestSupport.jwtWithRoles;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ProductController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    @DisplayName("GET /api/products returns all products when name is absent")
    void listAll() throws Exception {
        Product p = sampleProduct(1L, "Widget");
        when(productService.findAll()).thenReturn(List.of(p));

        mockMvc.perform(get("/api/products")
                        .with(jwtWithRoles("alice", "ROLE_USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Widget"));
    }

    @Test
    @DisplayName("GET /api/products?name=foo searches by name")
    void searchByName() throws Exception {
        Product p = sampleProduct(2L, "Foo Bar");
        when(productService.searchByName("foo")).thenReturn(List.of(p));

        mockMvc.perform(get("/api/products")
                        .param("name", "foo")
                        .with(jwtWithRoles("alice", "ROLE_USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(2));
    }

    @Test
    @DisplayName("GET /api/products/{id} returns one product")
    void getById() throws Exception {
        when(productService.findById(5L)).thenReturn(sampleProduct(5L, "Solo"));

        mockMvc.perform(get("/api/products/5")
                        .with(jwtWithRoles("alice", "ROLE_USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.name").value("Solo"));
    }

    @Test
    @DisplayName("GET without authentication is unauthorized")
    void unauthorizedWithoutJwt() throws Exception {
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isUnauthorized());
    }

    private static Product sampleProduct(Long id, String name) {
        LocalDateTime now = LocalDateTime.now();
        return Product.builder()
                .id(id)
                .name(name)
                .description("d")
                .price(BigDecimal.valueOf(9.99))
                .createdAt(now)
                .updatedAt(now)
                .build();
    }
}
