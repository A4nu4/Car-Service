package com.carrental.booking.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record RescheduleBookingRequest(
        @NotNull Instant pickupAt,
        @NotNull Instant dropoffAt
) {

    @AssertTrue(message = "dropoffAt must be after pickupAt")
    @Schema(hidden = true)
    public boolean isPeriodValid() {
        return pickupAt == null || dropoffAt == null || dropoffAt.isAfter(pickupAt);
    }
}
