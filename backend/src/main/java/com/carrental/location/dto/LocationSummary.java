package com.carrental.location.dto;

import com.carrental.location.Location;

import java.util.UUID;

/** Compact form embedded in car and booking responses. */
public record LocationSummary(UUID id, String name, String city) {

    public static LocationSummary from(Location location) {
        return new LocationSummary(location.getId(), location.getName(), location.getCity());
    }
}
