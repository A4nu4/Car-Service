package com.carrental.booking;

import java.math.BigDecimal;

public record PriceQuote(BigDecimal dailyRate, int rentalDays, BigDecimal basePrice) {
}
