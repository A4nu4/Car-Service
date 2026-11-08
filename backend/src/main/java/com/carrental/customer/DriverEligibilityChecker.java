package com.carrental.customer;

import com.carrental.common.exception.ApiException;
import com.carrental.common.exception.ErrorCode;
import com.carrental.config.RentalProperties;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;

/** Decides whether a customer may drive a rental car for a given period. */
@Component
public class DriverEligibilityChecker {

    private final RentalProperties rental;

    public DriverEligibilityChecker(RentalProperties rental) {
        this.rental = rental;
    }

    public void check(CustomerProfile profile, Instant pickupAt, Instant dropoffAt) {
        if (profile == null || !profile.isComplete()) {
            throw new ApiException(ErrorCode.DRIVER_NOT_ELIGIBLE,
                    "Complete the driver profile (date of birth and driving licence) before booking");
        }
        LocalDate pickupDate = LocalDate.ofInstant(pickupAt, rental.timeZone());
        LocalDate dropoffDate = LocalDate.ofInstant(dropoffAt, rental.timeZone());

        int ageAtPickup = Period.between(profile.getDateOfBirth(), pickupDate).getYears();
        if (ageAtPickup < rental.minDriverAge()) {
            throw new ApiException(ErrorCode.DRIVER_NOT_ELIGIBLE,
                    "The driver must be at least " + rental.minDriverAge() + " years old at pickup");
        }
        if (profile.getDriverLicenseExpiry().isBefore(dropoffDate)) {
            throw new ApiException(ErrorCode.DRIVER_NOT_ELIGIBLE,
                    "The driving licence expires before the end of the rental");
        }
    }
}
