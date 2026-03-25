package com.xyz.orders.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record PlaceOrderRequest(
        @NotBlank(message = "Customer name is mandatory")
        String customerName,

        @NotBlank(message = "Mobile number is mandatory")
        String mobileNumber,

        String shippingAddress,

        String email,

        @NotEmpty(message = "Order must contain at least one item")
        @Valid
        List<OrderItemRequest> items
) {
    public record OrderItemRequest(
            @jakarta.validation.constraints.NotNull(message = "Product ID is required")
            Long productId,

            @jakarta.validation.constraints.Min(value = 1, message = "Quantity must be at least 1")
            int quantity
    ) {}
}
