package com.carrental.booking.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record PickupRequest(
        @NotNull @Min(0) @Max(5_000_000) @Schema(description = "Odometer reading at handover, in km")
        Integer mileage
) {
}
