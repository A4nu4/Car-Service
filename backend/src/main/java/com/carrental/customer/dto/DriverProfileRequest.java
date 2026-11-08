package com.carrental.customer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record DriverProfileRequest(
        @NotNull @Past LocalDate dateOfBirth,
        @NotBlank @Size(max = 50) String driverLicenseNumber,
        @NotBlank @Pattern(regexp = "^[A-Z]{2}$", message = "must be an ISO 3166-1 alpha-2 country code, e.g. DE")
        String driverLicenseCountry,
        @NotNull LocalDate driverLicenseExpiry,
        @Size(max = 255) String addressLine,
        @Size(max = 100) String city,
        @Size(max = 20) String postalCode,
        @Pattern(regexp = "^[A-Z]{2}$", message = "must be an ISO 3166-1 alpha-2 country code, e.g. DE")
        String country
) {
}
