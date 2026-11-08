package com.carrental.booking.dto;

import com.carrental.booking.BookingStatus;
import com.carrental.booking.PaymentStatus;
import com.carrental.car.dto.CarSummary;
import com.carrental.location.dto.LocationSummary;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * @param freeCancellationUntil customers can cancel or reschedule themselves until this moment
 */
public record BookingResponse(
        UUID id,
        String reference,
        BookingStatus status,
        CarSummary car,
        CustomerInfo customer,
        LocationSummary pickupLocation,
        LocationSummary dropoffLocation,
        Instant pickupAt,
        Instant dropoffAt,
        Instant freeCancellationUntil,
        Price price,
        String notes,
        Handover handover,
        Instant cancelledAt,
        String cancellationReason,
        Instant createdAt,
        Instant updatedAt
) {

    public record CustomerInfo(UUID id, String name, String email, String phone) {
    }

    /**
     * @param totalPrice  what the customer owes: base + late fee + extras, or only the cancellation fee if cancelled
     * @param balanceDue  totalPrice - amountPaid; negative means a refund is due
     */
    public record Price(
            String currency,
            BigDecimal dailyRate,
            int rentalDays,
            BigDecimal basePrice,
            BigDecimal lateFee,
            BigDecimal extraCharges,
            BigDecimal cancellationFee,
            BigDecimal totalPrice,
            BigDecimal amountPaid,
            BigDecimal balanceDue,
            PaymentStatus paymentStatus
    ) {
    }

    /** What happened at the desk; null fields until the car is picked up / returned. */
    public record Handover(
            Instant actualPickupAt,
            Integer pickupMileage,
            Instant actualReturnAt,
            Integer returnMileage,
            String returnNotes
    ) {
    }
}
