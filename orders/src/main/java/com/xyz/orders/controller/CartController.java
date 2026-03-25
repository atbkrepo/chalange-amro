package com.xyz.orders.controller;

import com.xyz.orders.dto.AddToCartRequest;
import com.xyz.orders.dto.CartItemResponse;
import com.xyz.orders.dto.CartResponse;
import com.xyz.orders.model.Cart;
import com.xyz.orders.model.CartItem;
import com.xyz.orders.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@Tag(name = "Cart", description = "Shopping cart management (requires USER or ADMIN roles)")
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_USER')")
public class CartController {

    private final CartService cartService;

    @GetMapping
    @Operation(summary = "Get current user's cart")
    public ResponseEntity<CartResponse> getCart(@AuthenticationPrincipal Jwt jwt) {
        String username = jwt.getSubject();
        Cart cart = this.cartService.getOrCreateCart(username);
        List<CartItem> items = this.cartService.getCartItems(username);
        return ResponseEntity.ok(CartResponse.from(cart, items));
    }

    @PostMapping("/items")
    @Operation(summary = "Add a product to the cart", description = "Adds the specified product with quantity. If the product already exists in the cart, quantity is incremented.")
    public ResponseEntity<CartItemResponse> addToCart(@AuthenticationPrincipal Jwt jwt,
                                                      @Valid @RequestBody AddToCartRequest request) {
        String username = jwt.getSubject();
        CartItem item = this.cartService.addItem(username, request.productId(), request.quantity());
        return ResponseEntity.ok(CartItemResponse.from(item));
    }

    @PutMapping("/items/{productId}")
    @Operation(summary = "Update quantity of a cart item")
    public ResponseEntity<CartItemResponse> updateQuantity(@AuthenticationPrincipal Jwt jwt,
                                                           @PathVariable Long productId,
                                                           @RequestParam int quantity) {
        String username = jwt.getSubject();
        CartItem item = this.cartService.updateItemQuantity(username, productId, quantity);
        return ResponseEntity.ok(CartItemResponse.from(item));
    }

    @DeleteMapping("/items/{productId}")
    @Operation(summary = "Remove a product from the cart")
    public ResponseEntity<Void> removeItem(@AuthenticationPrincipal Jwt jwt, @PathVariable Long productId) {
        this.cartService.removeItem(jwt.getSubject(), productId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    @Operation(summary = "Clear the entire cart")
    public ResponseEntity<Void> clearCart(@AuthenticationPrincipal Jwt jwt) {
        this.cartService.clearCart(jwt.getSubject());
        return ResponseEntity.noContent().build();
    }
}
