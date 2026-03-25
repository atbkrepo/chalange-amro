package com.xyz.orders.controller;

import com.xyz.orders.dto.ProductResponse;
import com.xyz.orders.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Tag(name = "Products", description = "Product search and catalog (requires USER or ADMIN roles)")
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_USER')")
public class ProductController {

    private final ProductService productService;

    @GetMapping
    @Operation(summary = "Search products", description = "Search products by name or list all if no query is provided")
    public ResponseEntity<List<ProductResponse>> searchProducts(
            @Parameter(description = "Product name to search for (case-insensitive, partial match)")
            @RequestParam(required = false) String name) {

        List<ProductResponse> products;
        if (!StringUtils.isBlank(name)) {
            products = this.productService.searchByName(name).stream()
                    .map(ProductResponse::from)
                    .toList();
        } else {
            products = this.productService.findAll().stream()
                    .map(ProductResponse::from)
                    .toList();
        }
        return ResponseEntity.ok(products);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get product by ID")
    public ResponseEntity<ProductResponse> getProduct(@PathVariable Long id) {
        return ResponseEntity.ok(ProductResponse.from(this.productService.findById(id)));
    }
}
