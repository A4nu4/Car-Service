package com.carrental.payment.dto;

import com.carrental.payment.Payment;
import com.carrental.payment.PaymentMethod;
import com.carrental.payment.PaymentType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        UUID bookingId,
        PaymentType type,
        PaymentMethod method,
        BigDecimal amount,
        String reference,
        Instant paidAt,
        String recordedBy
) {

    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getBooking().getId(),
                payment.getType(),
                payment.getMethod(),
                payment.getAmount(),
                payment.getReference(),
                payment.getPaidAt(),
                payment.getRecordedBy().getFullName());
    }
}
