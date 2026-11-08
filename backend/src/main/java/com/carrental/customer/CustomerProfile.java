package com.carrental.customer;

import com.carrental.common.entity.BaseEntity;
import com.carrental.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.time.LocalDate;

/** Driver details needed to rent a car. Separate from {@link User} because staff accounts have none. */
@Entity
@Table(name = "customer_profiles")
public class CustomerProfile extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "driver_license_number")
    private String driverLicenseNumber;

    @Column(name = "driver_license_country")
    private String driverLicenseCountry;

    @Column(name = "driver_license_expiry")
    private LocalDate driverLicenseExpiry;

    @Column(name = "address_line")
    private String addressLine;

    private String city;

    @Column(name = "postal_code")
    private String postalCode;

    private String country;

    protected CustomerProfile() {
        // for JPA
    }

    public CustomerProfile(User user) {
        this.user = user;
    }

    public void update(LocalDate dateOfBirth, String driverLicenseNumber, String driverLicenseCountry,
                       LocalDate driverLicenseExpiry, String addressLine, String city, String postalCode,
                       String country) {
        this.dateOfBirth = dateOfBirth;
        this.driverLicenseNumber = driverLicenseNumber;
        this.driverLicenseCountry = driverLicenseCountry;
        this.driverLicenseExpiry = driverLicenseExpiry;
        this.addressLine = addressLine;
        this.city = city;
        this.postalCode = postalCode;
        this.country = country;
    }

    public boolean isComplete() {
        return dateOfBirth != null && driverLicenseNumber != null && driverLicenseExpiry != null;
    }

    public User getUser() {
        return user;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public String getDriverLicenseNumber() {
        return driverLicenseNumber;
    }

    public String getDriverLicenseCountry() {
        return driverLicenseCountry;
    }

    public LocalDate getDriverLicenseExpiry() {
        return driverLicenseExpiry;
    }

    public String getAddressLine() {
        return addressLine;
    }

    public String getCity() {
        return city;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public String getCountry() {
        return country;
    }
}
