package com.xyz.orders.controller;

import com.xyz.orders.dto.DailySalesAmount;
import com.xyz.orders.dto.ProductCatalogRow;
import com.xyz.orders.dto.ProductSalesRow;
import com.xyz.orders.service.SalesAnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
@Tag(name = "Analytics", description = "Sales analytics and inventory catalog (requires MANAGE or ADMIN roles)")
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_MANAGE')")
public class AnalyticsController {

    private final SalesAnalyticsService salesAnalyticsService;

    @GetMapping("/products/catalog")
    @Operation(summary = "Get product catalog with inventory availability")
    public ResponseEntity<List<ProductCatalogRow>> getProductCatalog() {
        return ResponseEntity.ok(this.salesAnalyticsService.findAllProductCatalogRows());
    }

    @GetMapping("/products/top-selling")
    @Operation(summary = "Top 5 selling products of a day")
    public ResponseEntity<List<ProductSalesRow>> topSellingProductsOfDay(
            @Parameter(description = "Day to report on (format: yyyy-MM-dd). If omitted, uses today.")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date,
            @Parameter(description = "Number of products to return (default: 5)")
            @RequestParam(required = false, defaultValue = "5")
            int limit) {

        if (date == null) {
            return ResponseEntity.ok(this.salesAnalyticsService.findTopSellingProductsOfDay());
        }
        return ResponseEntity.ok(this.salesAnalyticsService.findTopSellingProductsOfDay(date, limit));
    }

    @GetMapping("/products/least-selling")
    @Operation(summary = "Least selling products of a month")
    public ResponseEntity<List<ProductSalesRow>> leastSellingProductsOfMonth(
            @Parameter(description = "Month to report on (format: yyyy-MM). If omitted, uses current month.")
            @RequestParam(required = false) String month,
            @Parameter(description = "Number of products to return (default: 5)")
            @RequestParam(required = false, defaultValue = "5")
            int limit) {

        if (StringUtils.isBlank(month)) {
            return ResponseEntity.ok(this.salesAnalyticsService.findLeastSellingProductsOfMonth());
        }

        YearMonth yearMonth = YearMonth.parse(month);
        return ResponseEntity.ok(this.salesAnalyticsService.findLeastSellingProductsOfMonth(yearMonth, limit));
    }

    @GetMapping("/sales/amount-per-day")
    @Operation(summary = "Sale amount per day for a custom date range")
    public ResponseEntity<List<DailySalesAmount>> saleAmountPerDay(
            @Parameter(description = "Start date (format: yyyy-MM-dd)")
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate from,
            @Parameter(description = "End date (format: yyyy-MM-dd)")
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate to) {

        return ResponseEntity.ok(this.salesAnalyticsService.findSaleAmountPerDay(from, to));
    }
}

