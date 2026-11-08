package com.carrental.booking.dto;

import com.carrental.booking.BookingStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

/** Query parameters of GET /api/v1/bookings. For customers, only {@code status} applies. */
public record BookingFilter(
        BookingStatus status,
        @Schema(description = "Staff only") UUID customerId,
        @Schema(description = "Staff only") UUID carId,
        @Schema(description = "Staff only: pickup branch") UUID locationId,
        @Schema(description = "Pickup at or after this instant") Instant pickupFrom,
        @Schema(description = "Pickup before this instant") Instant pickupTo,
        @Schema(description = "Exact booking reference, e.g. BK-7KQ2M9XA") String reference
) {
}
