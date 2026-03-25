package com.xyz.orders.service;

import com.xyz.orders.dto.DailySalesAmount;
import com.xyz.orders.dto.ProductCatalogRow;
import com.xyz.orders.dto.ProductSalesRow;
import com.xyz.orders.model.OrderStatus;
import com.xyz.orders.repository.OrderItemRepository;
import com.xyz.orders.repository.OrderRepository;
import com.xyz.orders.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SalesAnalyticsService {

    private static final int DEFAULT_RANK_LIMIT = 5;

    private static final Set<OrderStatus> VALUED_ORDER_STATUSES = EnumSet.of(
            OrderStatus.CONFIRMED,
            OrderStatus.SHIPPED,
            OrderStatus.DELIVERED);

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;

    public List<ProductCatalogRow> findAllProductCatalogRows() {
        return this.productRepository.findAllProductCatalogRows();
    }

    private List<ProductSalesRow> findTopSellingProductsOfDay(LocalDate day, int limit) {
        LocalDateTime startDay = day.atStartOfDay();
        LocalDateTime endDay = day.atTime(LocalTime.MAX);
        return this.orderItemRepository.findTopSellingProductsByDay(
                startDay, endDay, VALUED_ORDER_STATUSES, PageRequest.of(0, limit)
        );
    }

    public List<ProductSalesRow> findTopSellingProductsOfDay() {
        return this.findTopSellingProductsOfDay(LocalDate.now(), DEFAULT_RANK_LIMIT);
    }

    private List<ProductSalesRow> findLeastSellingProductsOfMonth(YearMonth yearMonth, int limit) {
        LocalDateTime monthStart = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime monthEnd = yearMonth.atEndOfMonth().atTime(LocalTime.MAX);
        return this.orderItemRepository.findLeastSellingProductsByMonth(
                monthStart, monthEnd, VALUED_ORDER_STATUSES, PageRequest.of(0, limit)
        );
    }

    public List<ProductSalesRow> findLeastSellingProductsOfMonth() {
        return this.findLeastSellingProductsOfMonth(YearMonth.now(), DEFAULT_RANK_LIMIT);
    }


    public List<DailySalesAmount> findSaleAmountPerDay(LocalDate fromInclusive, LocalDate toInclusive) {
        if (toInclusive.isBefore(fromInclusive)) {
            throw new IllegalArgumentException("toDate must not be before fromDate");
        }
        LocalDateTime rangeStart = fromInclusive.atStartOfDay();
        LocalDateTime rangeEnd = toInclusive.atTime(LocalTime.MAX);
        return this.orderRepository.sumTotalAmountPerDay(
                rangeStart, rangeEnd, VALUED_ORDER_STATUSES
        );
    }
}
