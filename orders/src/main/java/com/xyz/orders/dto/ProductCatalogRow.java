package com.xyz.orders.dto;

import java.math.BigDecimal;

/**
 * Product catalog row with inventory: name, available quantity, price, and low-stock flag (below 10).
 */
public record ProductCatalogRow(
        String name,
        Integer quantityAvailable,
        BigDecimal price,
        boolean lowStock
) {
}
