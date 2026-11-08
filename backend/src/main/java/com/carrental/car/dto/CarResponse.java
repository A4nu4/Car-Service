package com.carrental.car.dto;

import com.carrental.car.Car;
import com.carrental.car.CarCategory;
import com.carrental.car.FuelType;
import com.carrental.car.Transmission;
import com.carrental.location.dto.LocationSummary;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Public, customer-facing view of a car. Internal fleet data (VIN, plate, mileage) is omitted.
 *
 * @param name display name, e.g. "Toyota Yaris"
 */
public record CarResponse(
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
        LocationSummary location
) {

    public static CarResponse from(Car car) {
        return new CarResponse(
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
                LocationSummary.from(car.getLocation()));
    }
}
