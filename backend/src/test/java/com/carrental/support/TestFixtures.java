package com.carrental.support;

import com.carrental.car.Car;
import com.carrental.car.CarCategory;
import com.carrental.car.FuelType;
import com.carrental.car.Transmission;
import com.carrental.config.RentalProperties;
import com.carrental.location.Location;
import com.carrental.user.Role;
import com.carrental.user.User;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.ZoneId;
import java.util.UUID;

/** Builders for plain domain objects in unit tests (no Spring, no database). */
public final class TestFixtures {

    private TestFixtures() {
    }

    public static RentalProperties rentalProperties() {
        return new RentalProperties(ZoneId.of("Europe/Berlin"), "EUR", 21, 30, 365, Duration.ofHours(24),
                Duration.ofMinutes(59));
    }

    public static <T> T withId(T entity) {
        ReflectionTestUtils.setField(entity, "id", UUID.randomUUID());
        return entity;
    }

    public static User user(Role role) {
        return withId(new User(UUID.randomUUID() + "@example.com", "hash", "Test", "User", null, role));
    }

    public static Location location() {
        return withId(new Location("Branch " + UUID.randomUUID(), "Street 1", "Berlin", "10115", "DE", null, null));
    }

    public static Car car(String pricePerDay) {
        Car.CarDetails details = new Car.CarDetails("VW", "Golf", 2024, CarCategory.ECONOMY, Transmission.MANUAL,
                FuelType.PETROL, 5, new BigDecimal(pricePerDay), null);
        return withId(new Car(details, "WVWZZZ1KZAW000001", "B-XX 1", 1000, location()));
    }
}
