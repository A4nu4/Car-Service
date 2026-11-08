package com.carrental.car;

import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.UUID;

/** Optional catalogue filters. Each returns null when its argument is null, meaning "no filter". */
public final class CarSpecifications {

    private CarSpecifications() {
    }

    public static Specification<Car> hasStatus(CarStatus status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<Car> atLocation(UUID locationId) {
        return (root, query, cb) -> locationId == null ? null : cb.equal(root.get("location").get("id"), locationId);
    }

    public static Specification<Car> atActiveLocation() {
        return (root, query, cb) -> cb.isTrue(root.get("location").get("active"));
    }

    public static Specification<Car> inCategory(CarCategory category) {
        return (root, query, cb) -> category == null ? null : cb.equal(root.get("category"), category);
    }

    public static Specification<Car> hasTransmission(Transmission transmission) {
        return (root, query, cb) -> transmission == null ? null : cb.equal(root.get("transmission"), transmission);
    }

    public static Specification<Car> hasAtLeastSeats(Integer seats) {
        return (root, query, cb) -> seats == null ? null : cb.greaterThanOrEqualTo(root.get("seats"), seats);
    }

    public static Specification<Car> costsAtMost(BigDecimal maxPricePerDay) {
        return (root, query, cb) -> maxPricePerDay == null
                ? null
                : cb.lessThanOrEqualTo(root.get("pricePerDay"), maxPricePerDay);
    }

    /** Admin search on brand, model or registration number. */
    public static Specification<Car> matchesText(String text) {
        return (root, query, cb) -> {
            if (text == null || text.isBlank()) {
                return null;
            }
            String pattern = "%" + text.trim().toLowerCase(Locale.ROOT) + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("brand")), pattern),
                    cb.like(cb.lower(root.get("model")), pattern),
                    cb.like(cb.lower(root.get("registrationNumber")), pattern));
        };
    }
}
