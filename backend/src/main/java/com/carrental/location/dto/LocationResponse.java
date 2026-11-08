package com.carrental.location.dto;

import com.carrental.location.Location;

import java.util.UUID;

public record LocationResponse(
        UUID id,
        String name,
        String addressLine,
        String city,
        String postalCode,
        String country,
        String phone,
        String openingHours,
        boolean active
) {

    public static LocationResponse from(Location location) {
        return new LocationResponse(
                location.getId(),
                location.getName(),
                location.getAddressLine(),
                location.getCity(),
                location.getPostalCode(),
                location.getCountry(),
                location.getPhone(),
                location.getOpeningHours(),
                location.isActive());
    }
}
