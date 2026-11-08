package com.carrental.dashboard.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * @param period first day of the day/month bucket
 */
public record RevenuePoint(LocalDate period, BigDecimal amount) {
}
