package com.carrental.car;

import com.carrental.common.entity.BaseEntity;
import com.carrental.location.Location;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/** A physical car in the fleet. Customers book a specific car. */
@Entity
@Table(name = "cars")
public class Car extends BaseEntity {

    @Column(nullable = false)
    private String brand;

    @Column(nullable = false)
    private String model;

    @Column(name = "model_year", nullable = false)
    private int modelYear;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CarCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Transmission transmission;

    @Enumerated(EnumType.STRING)
    @Column(name = "fuel_type", nullable = false)
    private FuelType fuelType;

    @Column(nullable = false)
    private int seats;

    @Column(name = "price_per_day", nullable = false, precision = 10, scale = 2)
    private BigDecimal pricePerDay;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(nullable = false, unique = true)
    private String vin;

    @Column(name = "registration_number", nullable = false, unique = true)
    private String registrationNumber;

    @Column(nullable = false)
    private int mileage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CarStatus status = CarStatus.AVAILABLE;

    /** The branch where the car currently is (changes when a one-way rental is returned elsewhere). */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "location_id", nullable = false)
    private Location location;

    protected Car() {
        // for JPA
    }

    public Car(CarDetails details, String vin, String registrationNumber, int mileage, Location location) {
        applyDetails(details);
        this.vin = vin;
        this.registrationNumber = registrationNumber;
        this.mileage = mileage;
        this.location = location;
    }

    public void update(CarDetails details, String vin, String registrationNumber, int mileage, Location location) {
        applyDetails(details);
        this.vin = vin;
        this.registrationNumber = registrationNumber;
        this.mileage = mileage;
        this.location = location;
    }

    private void applyDetails(CarDetails details) {
        this.brand = details.brand();
        this.model = details.model();
        this.modelYear = details.modelYear();
        this.category = details.category();
        this.transmission = details.transmission();
        this.fuelType = details.fuelType();
        this.seats = details.seats();
        this.pricePerDay = details.pricePerDay();
        this.imageUrl = details.imageUrl();
    }

    public void changeStatus(CarStatus status) {
        this.status = status;
    }

    /** Called when the car is handed over or returned; the odometer never goes backwards. */
    public void recordMileage(int newMileage) {
        if (newMileage > mileage) {
            this.mileage = newMileage;
        }
    }

    public void moveTo(Location newLocation) {
        this.location = newLocation;
    }

    public boolean isRentable() {
        return status == CarStatus.AVAILABLE && location.isActive();
    }

    public String getDisplayName() {
        return brand + " " + model;
    }

    public String getBrand() {
        return brand;
    }

    public String getModel() {
        return model;
    }

    public int getModelYear() {
        return modelYear;
    }

    public CarCategory getCategory() {
        return category;
    }

    public Transmission getTransmission() {
        return transmission;
    }

    public FuelType getFuelType() {
        return fuelType;
    }

    public int getSeats() {
        return seats;
    }

    public BigDecimal getPricePerDay() {
        return pricePerDay;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getVin() {
        return vin;
    }

    public String getRegistrationNumber() {
        return registrationNumber;
    }

    public int getMileage() {
        return mileage;
    }

    public CarStatus getStatus() {
        return status;
    }

    public Location getLocation() {
        return location;
    }

    /** The descriptive, customer-facing attributes of a car, grouped to keep constructors readable. */
    public record CarDetails(String brand, String model, int modelYear, CarCategory category,
                                   Transmission transmission, FuelType fuelType, int seats,
                                   BigDecimal pricePerDay, String imageUrl) {
    }
}
