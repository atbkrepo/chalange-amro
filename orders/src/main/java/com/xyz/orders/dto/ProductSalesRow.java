package com.xyz.orders.dto;

/**
 * Aggregated units sold per product (e.g. top / least sellers).
 */
public record ProductSalesRow(
        Long productId,
        String productName,
        Long totalQuantitySold
) {
}
