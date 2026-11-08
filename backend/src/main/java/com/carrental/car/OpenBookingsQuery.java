package com.carrental.car;

import java.util.UUID;

/**
 * Asks whether a car has confirmed or in-progress bookings. Defined here and implemented by the
 * booking module (dependency inversion): fleet management needs the answer, but the car package
 * must not depend on the booking package, which already depends on cars.
 */
public interface OpenBookingsQuery {

    boolean hasOpenBookings(UUID carId);
}
