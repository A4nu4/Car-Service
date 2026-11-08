package com.carrental.booking;

import com.carrental.support.TestFixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class PricingCalculatorTest {

    private final PricingCalculator calculator = new PricingCalculator(TestFixtures.rentalProperties());

    @ParameterizedTest(name = "{0} -> {1} = {2} day(s)")
    @CsvSource({
            "2026-11-10T09:00:00Z, 2026-11-10T10:00:00Z, 1",   // one hour still costs a day
            "2026-11-10T09:00:00Z, 2026-11-11T09:00:00Z, 1",   // exactly 24h
            "2026-11-10T09:00:00Z, 2026-11-11T09:00:01Z, 2",   // one second over starts a new day
            "2026-11-10T09:00:00Z, 2026-11-13T09:00:00Z, 3"
    })
    void rentalDaysArePerStarted24Hours(String pickup, String dropoff, int expectedDays) {
        assertThat(PricingCalculator.rentalDays(Instant.parse(pickup), Instant.parse(dropoff))).isEqualTo(expectedDays);
    }

    @Test
    void quoteMultipliesDailyRateByDays() {
        PriceQuote quote = calculator.quote(new BigDecimal("34.99"),
                Instant.parse("2026-11-10T09:00:00Z"), Instant.parse("2026-11-13T09:00:00Z"));

        assertThat(quote.rentalDays()).isEqualTo(3);
        assertThat(quote.basePrice()).isEqualByComparingTo("104.97");
        assertThat(quote.basePrice().scale()).isEqualTo(2);
    }

    @ParameterizedTest(name = "returned at {0} -> late fee {1}")
    @CsvSource({
            "2026-11-13T09:00:00Z, 0.00",    // on time
            "2026-11-13T09:59:00Z, 0.00",    // within the 59-minute grace period
            "2026-11-13T10:00:00Z, 50.00",   // one hour late: one extra day
            "2026-11-14T09:00:00Z, 50.00",   // exactly a day late
            "2026-11-14T09:30:00Z, 100.00"   // into the second late day
    })
    void lateFeeChargesStartedDaysAfterGracePeriod(String returnedAt, String expectedFee) {
        BigDecimal fee = calculator.lateFee(new BigDecimal("50.00"), Instant.parse("2026-11-13T09:00:00Z"),
                Instant.parse(returnedAt));

        assertThat(fee).isEqualByComparingTo(expectedFee);
    }

    @Test
    void earlyReturnHasNoLateFee() {
        BigDecimal fee = calculator.lateFee(new BigDecimal("50.00"), Instant.parse("2026-11-13T09:00:00Z"),
                Instant.parse("2026-11-12T09:00:00Z"));

        assertThat(fee).isEqualByComparingTo("0");
    }
}
