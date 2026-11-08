package com.carrental.car;

import com.carrental.car.dto.CarRequest;
import com.carrental.car.dto.FleetCarResponse;
import com.carrental.common.exception.ApiException;
import com.carrental.common.exception.ErrorCode;
import com.carrental.location.Location;
import com.carrental.location.LocationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

/** Fleet management performed by staff. Customer-facing search lives in the booking module. */
@Service
@Transactional
public class FleetService {

    private final CarRepository cars;
    private final LocationService locationService;
    private final OpenBookingsQuery openBookings;

    public FleetService(CarRepository cars, LocationService locationService, OpenBookingsQuery openBookings) {
        this.cars = cars;
        this.locationService = locationService;
        this.openBookings = openBookings;
    }

    @Transactional(readOnly = true)
    public Page<FleetCarResponse> list(CarStatus status, UUID locationId, CarCategory category, String search,
                                       Pageable pageable) {
        Specification<Car> spec = Specification.allOf(
                CarSpecifications.hasStatus(status),
                CarSpecifications.atLocation(locationId),
                CarSpecifications.inCategory(category),
                CarSpecifications.matchesText(search));
        return cars.findAll(spec, pageable).map(FleetCarResponse::from);
    }

    @Transactional(readOnly = true)
    public FleetCarResponse get(UUID id) {
        return FleetCarResponse.from(cars.findWithLocationById(id).orElseThrow(() -> ApiException.notFound("Car", id)));
    }

    public FleetCarResponse create(CarRequest request) {
        String vin = normalize(request.vin());
        String plate = normalize(request.registrationNumber());
        if (cars.existsByVin(vin)) {
            throw duplicate("VIN");
        }
        if (cars.existsByRegistrationNumber(plate)) {
            throw duplicate("registration number");
        }
        Location location = locationService.find(request.locationId());
        Car car = new Car(toDetails(request), vin, plate, request.mileage(), location);
        return FleetCarResponse.from(cars.save(car));
    }

    public FleetCarResponse update(UUID id, CarRequest request) {
        Car car = find(id);
        String vin = normalize(request.vin());
        String plate = normalize(request.registrationNumber());
        if (cars.existsByVinAndIdNot(vin, id)) {
            throw duplicate("VIN");
        }
        if (cars.existsByRegistrationNumberAndIdNot(plate, id)) {
            throw duplicate("registration number");
        }
        Location location = locationService.find(request.locationId());
        boolean moving = !location.getId().equals(car.getLocation().getId());
        if (moving && openBookings.hasOpenBookings(id)) {
            throw new ApiException(ErrorCode.INVALID_STATE_TRANSITION,
                    "The car has upcoming or active bookings at its current branch and cannot be moved");
        }
        car.update(toDetails(request), vin, plate, request.mileage(), location);
        return FleetCarResponse.from(car);
    }

    /**
     * Taking a car out of service is refused while it has upcoming or active bookings, so customers
     * are never left with a confirmed booking for a car that cannot be rented.
     */
    public FleetCarResponse changeStatus(UUID id, CarStatus status) {
        Car car = find(id);
        if (status != CarStatus.AVAILABLE && openBookings.hasOpenBookings(id)) {
            throw new ApiException(ErrorCode.INVALID_STATE_TRANSITION,
                    "Cancel or complete the car's upcoming and active bookings first");
        }
        car.changeStatus(status);
        return FleetCarResponse.from(car);
    }

    private Car find(UUID id) {
        return cars.findWithLocationById(id).orElseThrow(() -> ApiException.notFound("Car", id));
    }

    private static Car.CarDetails toDetails(CarRequest request) {
        String imageUrl = request.imageUrl() == null || request.imageUrl().isBlank() ? null : request.imageUrl().trim();
        return new Car.CarDetails(request.brand().trim(), request.model().trim(), request.year(), request.category(),
                request.transmission(), request.fuelType(), request.seats(), request.pricePerDay(), imageUrl);
    }

    private static String normalize(String value) {
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private static ApiException duplicate(String field) {
        return new ApiException(ErrorCode.DUPLICATE_RESOURCE, "Another car already has this " + field);
    }
}
