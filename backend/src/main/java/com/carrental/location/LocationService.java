package com.carrental.location;

import com.carrental.common.exception.ApiException;
import com.carrental.common.exception.ErrorCode;
import com.carrental.location.dto.LocationRequest;
import com.carrental.location.dto.LocationResponse;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class LocationService {

    private static final Sort BY_CITY_AND_NAME = Sort.by("city", "name");

    private final LocationRepository locations;

    public LocationService(LocationRepository locations) {
        this.locations = locations;
    }

    @Transactional(readOnly = true)
    public List<LocationResponse> listActive() {
        return locations.findByActiveTrue(BY_CITY_AND_NAME).stream().map(LocationResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<LocationResponse> listAll() {
        return locations.findAll(BY_CITY_AND_NAME).stream().map(LocationResponse::from).toList();
    }

    /** Public lookup: inactive branches are hidden from customers. */
    @Transactional(readOnly = true)
    public LocationResponse getActive(UUID id) {
        Location location = find(id);
        if (!location.isActive()) {
            throw ApiException.notFound("Location", id);
        }
        return LocationResponse.from(location);
    }

    public LocationResponse create(LocationRequest request) {
        if (locations.existsByNameIgnoreCase(request.name().trim())) {
            throw duplicateName();
        }
        Location location = new Location(request.name().trim(), request.addressLine().trim(), request.city().trim(),
                request.postalCode().trim(), request.country(), trimToNull(request.phone()),
                trimToNull(request.openingHours()));
        return LocationResponse.from(locations.save(location));
    }

    public LocationResponse update(UUID id, LocationRequest request) {
        Location location = find(id);
        if (locations.existsByNameIgnoreCaseAndIdNot(request.name().trim(), id)) {
            throw duplicateName();
        }
        location.update(request.name().trim(), request.addressLine().trim(), request.city().trim(),
                request.postalCode().trim(), request.country(), trimToNull(request.phone()),
                trimToNull(request.openingHours()));
        return LocationResponse.from(location);
    }

    /** Deactivating hides a branch from search and new bookings; existing bookings are unaffected. */
    public LocationResponse setActive(UUID id, boolean active) {
        Location location = find(id);
        location.setActive(active);
        return LocationResponse.from(location);
    }

    @Transactional(readOnly = true)
    public Location find(UUID id) {
        return locations.findById(id).orElseThrow(() -> ApiException.notFound("Location", id));
    }

    private static ApiException duplicateName() {
        return new ApiException(ErrorCode.DUPLICATE_RESOURCE, "A location with this name already exists");
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
