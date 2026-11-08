package com.carrental.car;

/**
 * Operational status set by staff. "Currently rented" is deliberately NOT a status: it is derived
 * from bookings, so the two can never disagree.
 */
public enum CarStatus {
    AVAILABLE,
    MAINTENANCE,
    RETIRED
}
