package com.carrental.booking;

import java.util.Set;

/**
 * <pre>
 * CONFIRMED --pickup--> ACTIVE --return--> COMPLETED
 *     |--cancel--> CANCELLED
 *     '--no-show--> NO_SHOW
 * </pre>
 */
public enum BookingStatus {
    CONFIRMED,
    ACTIVE,
    COMPLETED,
    CANCELLED,
    NO_SHOW;

    /** Statuses that occupy the car. Must match the WHERE clause of the ex_bookings_no_overlap constraint. */
    public static final Set<BookingStatus> BLOCKING = Set.of(CONFIRMED, ACTIVE);
}
