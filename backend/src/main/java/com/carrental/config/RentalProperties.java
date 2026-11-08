package com.carrental.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.time.ZoneId;

/**
 * Business rules for rentals. Kept in configuration so they can change without a code release.
 *
 * @param timeZone               zone used for calendar rules (driver age, licence expiry, "today" on the dashboard)
 * @param freeCancellationWindow customers may cancel or reschedule for free until this long before pickup
 * @param lateReturnGrace        returns later than the planned drop-off by more than this are charged extra days
 */
@Validated
@ConfigurationProperties(prefix = "app.rental")
public record RentalProperties(
        @NotNull ZoneId timeZone,
        @NotBlank @Pattern(regexp = "^[A-Z]{3}$") String currency,
        @Min(16) int minDriverAge,
        @Min(1) int maxRentalDays,
        @Min(1) int bookingHorizonDays,
        @NotNull Duration freeCancellationWindow,
        @NotNull Duration lateReturnGrace
) {
}
