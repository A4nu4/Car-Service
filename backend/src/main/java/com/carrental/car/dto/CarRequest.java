package com.carrental.car.dto;

import com.carrental.car.CarCategory;
import com.carrental.car.FuelType;
import com.carrental.car.Transmission;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

import java.math.BigDecimal;
import java.util.UUID;

public record CarRequest(
        @NotBlank @Size(max = 60) String brand,
        @NotBlank @Size(max = 60) String model,
        @NotNull @Min(1990) @Max(2100) Integer year,
        @NotNull CarCategory category,
        @NotNull Transmission transmission,
        @NotNull FuelType fuelType,
        @NotNull @Min(1) @Max(9) Integer seats,
        @NotNull @DecimalMin(value = "0.01") @Digits(integer = 8, fraction = 2) BigDecimal pricePerDay,
        @URL(protocol = "https") @Size(max = 500) String imageUrl,
        @NotBlank
        @Pattern(regexp = "^[A-HJ-NPR-Z0-9]{17}$", message = "must be 17 characters (letters except I, O, Q, and digits)")
        String vin,
        @NotBlank @Size(max = 20) String registrationNumber,
        @NotNull @Min(0) Integer mileage,
        @NotNull UUID locationId
) {
}
