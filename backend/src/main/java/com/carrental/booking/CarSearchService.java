package com.carrental.booking;

import com.carrental.booking.dto.CarSearchParams;
import com.carrental.booking.dto.PriceQuoteResponse;
import com.carrental.car.Car;
import com.carrental.car.CarRepository;
import com.carrental.car.CarSpecifications;
import com.carrental.car.CarStatus;
import com.carrental.car.dto.CarResponse;
import com.carrental.common.exception.ApiException;
import com.carrental.config.RentalProperties;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Customer-facing car search. It lives in the booking module because "is this car free?"
 * is answered by bookings, and the car module must not depend on bookings.
 */
@Service
@Transactional(readOnly = true)
public class CarSearchService {

    private final CarRepository cars;
    private final BookingRepository bookings;
    private final PricingCalculator pricing;
    private final RentalProperties rental;

    public CarSearchService(CarRepository cars, BookingRepository bookings, PricingCalculator pricing,
                            RentalProperties rental) {
        this.cars = cars;
        this.bookings = bookings;
        this.pricing = pricing;
        this.rental = rental;
    }

    public Page<CarResponse> search(CarSearchParams params, Pageable pageable) {
        Specification<Car> spec = Specification.allOf(
                CarSpecifications.hasStatus(CarStatus.AVAILABLE),
                CarSpecifications.atActiveLocation(),
                CarSpecifications.atLocation(params.locationId()),
                CarSpecifications.inCategory(params.category()),
                CarSpecifications.hasTransmission(params.transmission()),
                CarSpecifications.hasAtLeastSeats(params.minSeats()),
                CarSpecifications.costsAtMost(params.maxPricePerDay()));
        if (params.hasPeriod()) {
            spec = spec.and(BookingSpecifications.carAvailableBetween(params.pickupAt(), params.dropoffAt()));
        }
        return cars.findAll(spec, pageable).map(CarResponse::from);
    }

    public CarResponse get(UUID id) {
        return CarResponse.from(findRentable(id));
    }

    public PriceQuoteResponse quote(UUID carId, Instant pickupAt, Instant dropoffAt) {
        if (!dropoffAt.isAfter(pickupAt)) {
            throw ApiException.businessRule("dropoffAt must be after pickupAt");
        }
        Car car = findRentable(carId);
        boolean available = !bookings.existsOverlapping(carId, pickupAt, dropoffAt, BookingStatus.BLOCKING);
        PriceQuote quote = pricing.quote(car.getPricePerDay(), pickupAt, dropoffAt);
        return new PriceQuoteResponse(carId, pickupAt, dropoffAt, available, rental.currency(), quote.dailyRate(),
                quote.rentalDays(), quote.basePrice());
    }

    private Car findRentable(UUID id) {
        return cars.findWithLocationById(id)
                .filter(Car::isRentable)
                .orElseThrow(() -> ApiException.notFound("Car", id));
    }
}
