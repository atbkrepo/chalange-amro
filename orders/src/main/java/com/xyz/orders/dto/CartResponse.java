package com.xyz.orders.dto;

import com.xyz.orders.model.Cart;
import com.xyz.orders.model.CartItem;

import java.util.List;

public record CartResponse(
        Long id,
        String username,
        List<CartItemResponse> items
) {
    public static CartResponse from(Cart cart, List<CartItem> items) {
        return new CartResponse(
                cart.getId(),
                cart.getUsername(),
                items.stream().map(CartItemResponse::from).toList()
        );
    }
}
