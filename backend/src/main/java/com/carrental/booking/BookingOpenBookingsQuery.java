package com.carrental.booking;

import com.carrental.car.OpenBookingsQuery;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
class BookingOpenBookingsQuery implements OpenBookingsQuery {

    private final BookingRepository bookings;

    BookingOpenBookingsQuery(BookingRepository bookings) {
        this.bookings = bookings;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasOpenBookings(UUID carId) {
        return bookings.existsByCarIdAndStatusIn(carId, BookingStatus.BLOCKING);
    }
}
