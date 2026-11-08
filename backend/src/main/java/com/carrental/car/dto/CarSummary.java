package com.carrental.car.dto;

import com.carrental.car.Car;
import com.carrental.car.CarCategory;

import java.util.UUID;

/** Compact form embedded in booking responses. */
public record CarSummary(UUID id, String name, CarCategory category, String registrationNumber, String imageUrl) {

    public static CarSummary from(Car car) {
        return new CarSummary(car.getId(), car.getDisplayName(), car.getCategory(), car.getRegistrationNumber(),
                car.getImageUrl());
    }
}
