package com.carrental.booking.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ReturnRequest(
        @NotNull @Min(0) @Max(5_000_000) @Schema(description = "Odometer reading at return, in km")
        Integer mileage,
        @DecimalMin("0.00") @Digits(integer = 10, fraction = 2)
        @Schema(description = "Damage, refuelling, cleaning etc. Late fees are calculated automatically.")
        BigDecimal extraCharges,
        @Size(max = 1000) String notes
) {
}
