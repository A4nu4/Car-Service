package com.carrental.customer.dto;

import com.carrental.customer.CustomerProfile;

import java.time.LocalDate;

/**
 * @param complete true when the profile has everything required to book a car
 */
public record DriverProfileResponse(
        LocalDate dateOfBirth,
        String driverLicenseNumber,
        String driverLicenseCountry,
        LocalDate driverLicenseExpiry,
        String addressLine,
        String city,
        String postalCode,
        String country,
        boolean complete
) {

    public static DriverProfileResponse from(CustomerProfile profile) {
        return new DriverProfileResponse(
                profile.getDateOfBirth(),
                profile.getDriverLicenseNumber(),
                profile.getDriverLicenseCountry(),
                profile.getDriverLicenseExpiry(),
                profile.getAddressLine(),
                profile.getCity(),
                profile.getPostalCode(),
                profile.getCountry(),
                profile.isComplete());
    }
}
