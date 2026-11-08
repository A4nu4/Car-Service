package com.carrental.dashboard.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record DashboardSummary(
        Instant generatedAt,
        Fleet fleet,
        Today today,
        Bookings bookings,
        Finance finance
) {

    /**
     * @param rentedNow cars currently out with a customer (ACTIVE bookings)
     */
    public record Fleet(long total, long available, long maintenance, long retired, long rentedNow) {
    }

    /**
     * @param pickupsScheduled CONFIRMED bookings starting today (rental time zone)
     * @param returnsDue       ACTIVE bookings due back today
     * @param overdueReturns   ACTIVE bookings whose drop-off time has passed
     */
    public record Today(long pickupsScheduled, long returnsDue, long overdueReturns) {
    }

    public record Bookings(long confirmed, long active) {
    }

    /**
     * @param revenueThisMonth   net payments received since the 1st of this month
     * @param outstandingBalance unpaid amounts on finished bookings
     */
    public record Finance(String currency, BigDecimal revenueThisMonth, BigDecimal outstandingBalance) {
    }
}
