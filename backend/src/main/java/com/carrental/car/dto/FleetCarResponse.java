package com.carrental.car.dto;

import com.carrental.car.Car;
import com.carrental.car.CarCategory;
import com.carrental.car.CarStatus;
import com.carrental.car.FuelType;
import com.carrental.car.Transmission;
import com.carrental.location.dto.LocationSummary;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Staff view of a car, including internal fleet data. */
public record FleetCarResponse(
        UUID id,
        String name,
        String brand,
        String model,
        int year,
        CarCategory category,
        Transmission transmission,
        FuelType fuelType,
        int seats,
        BigDecimal pricePerDay,
        String imageUrl,
        String vin,
        String registrationNumber,
        int mileage,
        CarStatus status,
        LocationSummary location,
        Instant createdAt,
        Instant updatedAt
) {

    public static FleetCarResponse from(Car car) {
        return new FleetCarResponse(
                car.getId(),
                car.getDisplayName(),
                car.getBrand(),
                car.getModel(),
                car.getModelYear(),
                car.getCategory(),
                car.getTransmission(),
                car.getFuelType(),
                car.getSeats(),
                car.getPricePerDay(),
                car.getImageUrl(),
                car.getVin(),
                car.getRegistrationNumber(),
                car.getMileage(),
                car.getStatus(),
                LocationSummary.from(car.getLocation()),
                car.getCreatedAt(),
                car.getUpdatedAt());
    }
}
