package com.carrental.customer;

import com.carrental.common.exception.ApiException;
import com.carrental.common.exception.ErrorCode;
import com.carrental.support.TestFixtures;
import com.carrental.user.Role;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DriverEligibilityCheckerTest {

    private static final Instant PICKUP = Instant.parse("2026-11-10T09:00:00Z");
    private static final Instant DROPOFF = Instant.parse("2026-11-15T09:00:00Z");

    private final DriverEligibilityChecker checker = new DriverEligibilityChecker(TestFixtures.rentalProperties());

    @Test
    void eligibleDriverPasses() {
        assertThatCode(() -> checker.check(profile(LocalDate.of(1990, 1, 1), LocalDate.of(2030, 1, 1)), PICKUP, DROPOFF))
                .doesNotThrowAnyException();
    }

    @Test
    void incompleteProfileIsRejected() {
        CustomerProfile empty = new CustomerProfile(TestFixtures.user(Role.CUSTOMER));

        assertNotEligible(() -> checker.check(empty, PICKUP, DROPOFF));
    }

    @Test
    void driverTurning21AfterPickupIsRejected() {
        assertNotEligible(() -> checker.check(profile(LocalDate.of(2005, 11, 11), LocalDate.of(2030, 1, 1)),
                PICKUP, DROPOFF));
    }

    @Test
    void driverTurning21OnPickupDayIsAccepted() {
        assertThatCode(() -> checker.check(profile(LocalDate.of(2005, 11, 10), LocalDate.of(2030, 1, 1)),
                PICKUP, DROPOFF)).doesNotThrowAnyException();
    }

    @Test
    void licenceExpiringDuringRentalIsRejected() {
        assertNotEligible(() -> checker.check(profile(LocalDate.of(1990, 1, 1), LocalDate.of(2026, 11, 14)),
                PICKUP, DROPOFF));
    }

    private static CustomerProfile profile(LocalDate dateOfBirth, LocalDate licenceExpiry) {
        CustomerProfile profile = new CustomerProfile(TestFixtures.user(Role.CUSTOMER));
        profile.update(dateOfBirth, "B072RRE2I55", "DE", licenceExpiry, null, null, null, null);
        return profile;
    }

    private static void assertNotEligible(org.assertj.core.api.ThrowableAssert.ThrowingCallable call) {
        assertThatThrownBy(call)
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getCode())
                .isEqualTo(ErrorCode.DRIVER_NOT_ELIGIBLE);
    }
}
