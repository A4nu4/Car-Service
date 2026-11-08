package com.carrental.booking.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * @param available whether the car can be booked for this period right now (not a reservation)
 */
public record PriceQuoteResponse(
        UUID carId,
        Instant pickupAt,
        Instant dropoffAt,
        boolean available,
        String currency,
        BigDecimal dailyRate,
        int rentalDays,
        BigDecimal totalPrice
) {
}
