package com.carrental.booking.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public record CreateBookingRequest(
        @NotNull UUID carId,
        @NotNull @Schema(example = "2026-12-01T09:00:00Z") Instant pickupAt,
        @NotNull @Schema(example = "2026-12-04T09:00:00Z") Instant dropoffAt,
        @Schema(description = "Return branch; defaults to the car's pickup branch (round trip)")
        UUID dropoffLocationId,
        @Schema(description = "Staff only: the customer this booking is for. Ignored for customers.")
        UUID customerId,
        @Size(max = 1000) String notes
) {

    @AssertTrue(message = "dropoffAt must be after pickupAt")
    @Schema(hidden = true)
    public boolean isPeriodValid() {
        return pickupAt == null || dropoffAt == null || dropoffAt.isAfter(pickupAt);
    }
}
