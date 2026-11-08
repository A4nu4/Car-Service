package com.carrental.booking;

import com.carrental.car.Car;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

public final class BookingSpecifications {

    private BookingSpecifications() {
    }

    public static Specification<Booking> hasStatus(BookingStatus status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<Booking> forCustomer(UUID customerId) {
        return (root, query, cb) -> customerId == null ? null : cb.equal(root.get("customer").get("id"), customerId);
    }

    public static Specification<Booking> forCar(UUID carId) {
        return (root, query, cb) -> carId == null ? null : cb.equal(root.get("car").get("id"), carId);
    }

    public static Specification<Booking> pickupAtLocation(UUID locationId) {
        return (root, query, cb) -> locationId == null
                ? null
                : cb.equal(root.get("pickupLocation").get("id"), locationId);
    }

    public static Specification<Booking> pickupFrom(Instant from) {
        return (root, query, cb) -> from == null ? null : cb.greaterThanOrEqualTo(root.get("pickupAt"), from);
    }

    public static Specification<Booking> pickupBefore(Instant to) {
        return (root, query, cb) -> to == null ? null : cb.lessThan(root.get("pickupAt"), to);
    }

    public static Specification<Booking> hasReference(String reference) {
        return (root, query, cb) -> reference == null || reference.isBlank()
                ? null
                : cb.equal(root.get("reference"), reference.trim().toUpperCase(Locale.ROOT));
    }

    /**
     * Cars with no CONFIRMED or ACTIVE booking overlapping [from, to). Two periods overlap when each
     * starts before the other ends.
     */
    public static Specification<Car> carAvailableBetween(Instant from, Instant to) {
        return (root, query, cb) -> {
            Subquery<UUID> overlapping = query.subquery(UUID.class);
            Root<Booking> booking = overlapping.from(Booking.class);
            overlapping.select(booking.get("id")).where(
                    cb.equal(booking.get("car"), root),
                    booking.get("status").in(BookingStatus.BLOCKING),
                    cb.lessThan(booking.get("pickupAt"), to),
                    cb.greaterThan(booking.get("dropoffAt"), from));
            return cb.not(cb.exists(overlapping));
        };
    }
}
