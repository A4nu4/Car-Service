package com.carrental.booking.dto;

import com.carrental.car.CarCategory;
import com.carrental.car.Transmission;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Query parameters of GET /api/v1/cars. Give both {@code pickupAt} and {@code dropoffAt} to see only
 * cars that are free for that period; omit both to browse the catalogue.
 */
public record CarSearchParams(
        @Schema(description = "Pickup branch") UUID locationId,
        @Schema(description = "ISO-8601 instant, e.g. 2026-12-01T09:00:00Z") Instant pickupAt,
        @Schema(description = "ISO-8601 instant, e.g. 2026-12-04T09:00:00Z") Instant dropoffAt,
        CarCategory category,
        Transmission transmission,
        @Min(1) @Max(9) Integer minSeats,
        @DecimalMin("0") BigDecimal maxPricePerDay
) {

    @AssertTrue(message = "pickupAt and dropoffAt must be given together, with dropoffAt after pickupAt")
    @Schema(hidden = true)
    public boolean isPeriodValid() {
        if (pickupAt == null && dropoffAt == null) {
            return true;
        }
        return pickupAt != null && dropoffAt != null && dropoffAt.isAfter(pickupAt);
    }

    public boolean hasPeriod() {
        return pickupAt != null && dropoffAt != null;
    }
}
