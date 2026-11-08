package com.carrental.booking;

import com.carrental.common.exception.ApiException;
import com.carrental.common.exception.ErrorCode;
import com.carrental.support.TestFixtures;
import com.carrental.user.Role;
import com.carrental.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BookingTest {

    private static final Instant PICKUP = Instant.parse("2026-11-10T09:00:00Z");
    private static final Instant DROPOFF = Instant.parse("2026-11-12T09:00:00Z");

    private Booking booking;

    @BeforeEach
    void setUp() {
        User customer = TestFixtures.user(Role.CUSTOMER);
        var car = TestFixtures.car("50.00");
        PriceQuote quote = new PriceQuote(new BigDecimal("50.00"), 2, new BigDecimal("100.00"));
        booking = new Booking("BK-TEST0001", customer, car, car.getLocation(), PICKUP, DROPOFF, quote, null, customer);
    }

    @Test
    void newBookingIsConfirmedAndUnpaid() {
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(booking.getTotalPrice()).isEqualByComparingTo("100.00");
        assertThat(booking.getPaymentStatus()).isEqualTo(PaymentStatus.UNPAID);
    }

    @Test
    void completedRentalIncludesLateFeeAndExtras() {
        booking.startRental(PICKUP, 1000);
        booking.completeRental(DROPOFF, 1200, new BigDecimal("50.00"), new BigDecimal("12.50"), "scratch");

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.COMPLETED);
        assertThat(booking.getTotalPrice()).isEqualByComparingTo("162.50");
    }

    @Test
    void cancelledBookingOnlyOwesCancellationFee() {
        booking.cancel(PICKUP.minusSeconds(3600), "changed plans", new BigDecimal("50.00"));

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(booking.getTotalPrice()).isEqualByComparingTo("50.00");
    }

    @Test
    void illegalTransitionsAreRejected() {
        assertThatThrownBy(() -> booking.completeRental(DROPOFF, 1200, BigDecimal.ZERO, BigDecimal.ZERO, null))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getCode())
                .isEqualTo(ErrorCode.INVALID_STATE_TRANSITION);

        booking.cancel(PICKUP, null, BigDecimal.ZERO);
        assertThatThrownBy(() -> booking.startRental(PICKUP, 1000)).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> booking.cancel(PICKUP, null, BigDecimal.ZERO)).isInstanceOf(ApiException.class);
    }

    @Test
    void returnMileageCannotBeBelowPickupMileage() {
        booking.startRental(PICKUP, 1000);

        assertThatThrownBy(() -> booking.completeRental(DROPOFF, 999, BigDecimal.ZERO, BigDecimal.ZERO, null))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getCode())
                .isEqualTo(ErrorCode.BUSINESS_RULE_VIOLATION);
    }

    @Test
    void paymentStatusFollowsAmountPaid() {
        booking.recordCharge(new BigDecimal("40.00"));
        assertThat(booking.getPaymentStatus()).isEqualTo(PaymentStatus.PARTIALLY_PAID);
        assertThat(booking.getBalanceDue()).isEqualByComparingTo("60.00");

        booking.recordCharge(new BigDecimal("60.00"));
        assertThat(booking.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);

        booking.cancel(PICKUP.minusSeconds(86400 * 2), null, BigDecimal.ZERO);
        assertThat(booking.getPaymentStatus()).isEqualTo(PaymentStatus.REFUND_DUE);

        booking.recordRefund(new BigDecimal("100.00"));
        assertThat(booking.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
    }

    @Test
    void cannotRefundMoreThanPaid() {
        booking.recordCharge(new BigDecimal("20.00"));

        assertThatThrownBy(() -> booking.recordRefund(new BigDecimal("20.01"))).isInstanceOf(ApiException.class);
    }
}
