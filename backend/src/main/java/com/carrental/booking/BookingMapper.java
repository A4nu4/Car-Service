package com.carrental.booking;

import com.carrental.booking.dto.BookingResponse;
import com.carrental.car.dto.CarSummary;
import com.carrental.config.RentalProperties;
import com.carrental.location.dto.LocationSummary;
import com.carrental.user.User;
import org.springframework.stereotype.Component;

@Component
public class BookingMapper {

    private final RentalProperties rental;

    public BookingMapper(RentalProperties rental) {
        this.rental = rental;
    }

    public BookingResponse toResponse(Booking booking) {
        User customer = booking.getCustomer();
        return new BookingResponse(
                booking.getId(),
                booking.getReference(),
                booking.getStatus(),
                CarSummary.from(booking.getCar()),
                new BookingResponse.CustomerInfo(customer.getId(), customer.getFullName(), customer.getEmail(),
                        customer.getPhone()),
                LocationSummary.from(booking.getPickupLocation()),
                LocationSummary.from(booking.getDropoffLocation()),
                booking.getPickupAt(),
                booking.getDropoffAt(),
                booking.getPickupAt().minus(rental.freeCancellationWindow()),
                new BookingResponse.Price(
                        rental.currency(),
                        booking.getDailyRate(),
                        booking.getRentalDays(),
                        booking.getBasePrice(),
                        booking.getLateFee(),
                        booking.getExtraCharges(),
                        booking.getCancellationFee(),
                        booking.getTotalPrice(),
                        booking.getAmountPaid(),
                        booking.getBalanceDue(),
                        booking.getPaymentStatus()),
                booking.getCustomerNotes(),
                new BookingResponse.Handover(
                        booking.getActualPickupAt(),
                        booking.getPickupMileage(),
                        booking.getActualReturnAt(),
                        booking.getReturnMileage(),
                        booking.getReturnNotes()),
                booking.getCancelledAt(),
                booking.getCancellationReason(),
                booking.getCreatedAt(),
                booking.getUpdatedAt());
    }
}
