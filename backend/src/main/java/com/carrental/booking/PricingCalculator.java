package com.carrental.booking;

import com.carrental.config.RentalProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;

/**
 * All money rules in one pure, easily unit-tested class.
 * <ul>
 *   <li>A rental is charged per started 24-hour period, minimum one day.</li>
 *   <li>Returning later than the planned drop-off plus a grace period costs one daily rate per started late day.</li>
 *   <li>Late cancellations and no-shows cost one daily rate.</li>
 * </ul>
 */
@Component
public class PricingCalculator {

    private static final long SECONDS_PER_DAY = Duration.ofDays(1).toSeconds();

    private final RentalProperties rental;

    public PricingCalculator(RentalProperties rental) {
        this.rental = rental;
    }

    public PriceQuote quote(BigDecimal dailyRate, Instant pickupAt, Instant dropoffAt) {
        int days = rentalDays(pickupAt, dropoffAt);
        return new PriceQuote(money(dailyRate), days, money(dailyRate.multiply(BigDecimal.valueOf(days))));
    }

    public static int rentalDays(Instant pickupAt, Instant dropoffAt) {
        long seconds = Duration.between(pickupAt, dropoffAt).toSeconds();
        long days = (seconds + SECONDS_PER_DAY - 1) / SECONDS_PER_DAY;
        return (int) Math.max(1, days);
    }

    public BigDecimal lateFee(BigDecimal dailyRate, Instant plannedDropoffAt, Instant actualReturnAt) {
        Duration late = Duration.between(plannedDropoffAt, actualReturnAt);
        if (late.compareTo(rental.lateReturnGrace()) <= 0) {
            return money(BigDecimal.ZERO);
        }
        long lateDays = (late.toSeconds() + SECONDS_PER_DAY - 1) / SECONDS_PER_DAY;
        return money(dailyRate.multiply(BigDecimal.valueOf(lateDays)));
    }

    public BigDecimal lateCancellationFee(BigDecimal dailyRate) {
        return money(dailyRate);
    }

    public static BigDecimal money(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP);
    }
}
