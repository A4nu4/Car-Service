package com.carrental.location;

import com.carrental.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** A rental branch where cars are picked up and dropped off. */
@Entity
@Table(name = "locations")
public class Location extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String name;

    @Column(name = "address_line", nullable = false)
    private String addressLine;

    @Column(nullable = false)
    private String city;

    @Column(name = "postal_code", nullable = false)
    private String postalCode;

    @Column(nullable = false)
    private String country;

    private String phone;

    @Column(name = "opening_hours")
    private String openingHours;

    @Column(nullable = false)
    private boolean active = true;

    protected Location() {
        // for JPA
    }

    public Location(String name, String addressLine, String city, String postalCode, String country, String phone,
                    String openingHours) {
        update(name, addressLine, city, postalCode, country, phone, openingHours);
    }

    public final void update(String name, String addressLine, String city, String postalCode, String country,
                             String phone, String openingHours) {
        this.name = name;
        this.addressLine = addressLine;
        this.city = city;
        this.postalCode = postalCode;
        this.country = country;
        this.phone = phone;
        this.openingHours = openingHours;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getName() {
        return name;
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

    public String getPhone() {
        return phone;
    }

    public String getOpeningHours() {
        return openingHours;
    }

    public boolean isActive() {
        return active;
    }
}
