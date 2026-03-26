package com.xyz.orders.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xyz.orders.config.SecurityConfig;
import com.xyz.orders.dto.AddToCartRequest;
import com.xyz.orders.exception.GlobalExceptionHandler;
import com.xyz.orders.model.Cart;
import com.xyz.orders.model.CartItem;
import com.xyz.orders.model.Product;
import com.xyz.orders.service.CartService;
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
import java.util.List;

import static com.xyz.orders.controller.ControllerTestSupport.jwtWithRoles;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CartController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CartService cartService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    @DisplayName("GET /api/cart returns cart for subject")
    void getCart() throws Exception {
        Cart cart = Cart.builder().id(10L).username("bob").build();
        CartItem item = cartItem(1L, 3L, "P1");
        when(cartService.getOrCreateCart("bob")).thenReturn(cart);
        when(cartService.getCartItems("bob")).thenReturn(List.of(item));

        mockMvc.perform(get("/api/cart")
                        .with(jwtWithRoles("bob", "ROLE_USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("bob"))
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].quantity").value(2));
    }

    @Test
    @DisplayName("POST /api/cart/items adds line")
    void addToCart() throws Exception {
        CartItem saved = cartItem(5L, 7L, "Added");
        when(cartService.addItem(eq("bob"), eq(7L), eq(3))).thenReturn(saved);

        mockMvc.perform(post("/api/cart/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AddToCartRequest(7L, 3)))
                        .with(jwtWithRoles("bob", "ROLE_USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(7));

        verify(cartService).addItem("bob", 7L, 3);
    }

    @Test
    @DisplayName("PUT /api/cart/items/{productId} updates quantity")
    void updateQuantity() throws Exception {
        CartItem updated = cartItem(1L, 9L, "X");
        when(cartService.updateItemQuantity("bob", 9L, 4)).thenReturn(updated);

        mockMvc.perform(put("/api/cart/items/9")
                        .param("quantity", "4")
                        .with(jwtWithRoles("bob", "ROLE_USER")))
                .andExpect(status().isOk());

        verify(cartService).updateItemQuantity("bob", 9L, 4);
    }

    @Test
    @DisplayName("DELETE /api/cart/items/{productId} returns 204")
    void removeItem() throws Exception {
        mockMvc.perform(delete("/api/cart/items/9")
                        .with(jwtWithRoles("bob", "ROLE_USER")))
                .andExpect(status().isNoContent());

        verify(cartService).removeItem("bob", 9L);
    }

    @Test
    @DisplayName("DELETE /api/cart clears cart")
    void clearCart() throws Exception {
        mockMvc.perform(delete("/api/cart")
                        .with(jwtWithRoles("bob", "ROLE_USER")))
                .andExpect(status().isNoContent());

        verify(cartService).clearCart("bob");
    }

    @Test
    @DisplayName("POST /api/cart/items with invalid body returns 400")
    void addToCartValidation() throws Exception {
        mockMvc.perform(post("/api/cart/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":null,\"quantity\":0}")
                        .with(jwtWithRoles("bob", "ROLE_USER")))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("unauthenticated requests are rejected")
    void unauthorized() throws Exception {
        mockMvc.perform(get("/api/cart"))
                .andExpect(status().isUnauthorized());
    }

    private static CartItem cartItem(Long id, Long productId, String productName) {
        Product p = Product.builder()
                .id(productId)
                .name(productName)
                .price(BigDecimal.ONE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        Cart c = Cart.builder().id(1L).username("bob").build();
        return CartItem.builder()
                .id(id)
                .cart(c)
                .product(p)
                .quantity(2)
                .addedAt(LocalDateTime.now())
                .build();
    }
}
