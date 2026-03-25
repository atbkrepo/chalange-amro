package com.xyz.orders.dto;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;

/**
 * Total order amount aggregated per calendar day.
 */
public record DailySalesAmount(LocalDate day, BigDecimal totalAmount) {

    public DailySalesAmount(Date sqlDay, BigDecimal totalAmount) {
        this(sqlDay == null ? null : sqlDay.toLocalDate(), totalAmount);
    }
}
