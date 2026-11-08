package com.carrental.user;

/**
 * ADMIN manages fleet, branches, staff and sees statistics.
 * AGENT works the rental desk: bookings on behalf of customers, pickups, returns, payments.
 * CUSTOMER books and manages their own rentals.
 */
public enum Role {
    ADMIN,
    AGENT,
    CUSTOMER;

    public boolean isStaff() {
        return this == ADMIN || this == AGENT;
    }
}
