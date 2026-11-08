package com.carrental.payment;

import com.carrental.booking.Booking;
import com.carrental.booking.BookingMapper;
import com.carrental.booking.BookingService;
import com.carrental.booking.PricingCalculator;
import com.carrental.booking.dto.BookingResponse;
import com.carrental.payment.dto.PaymentResponse;
import com.carrental.payment.dto.RecordPaymentRequest;
import com.carrental.security.AuthenticatedUser;
import com.carrental.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class PaymentService {

    private final PaymentRepository payments;
    private final BookingService bookingService;
    private final BookingMapper bookingMapper;
    private final UserRepository users;
    private final Clock clock;

    public PaymentService(PaymentRepository payments, BookingService bookingService, BookingMapper bookingMapper,
                          UserRepository users, Clock clock) {
        this.payments = payments;
        this.bookingService = bookingService;
        this.bookingMapper = bookingMapper;
        this.users = users;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> list(UUID bookingId, AuthenticatedUser actor) {
        Booking booking = bookingService.findAccessible(bookingId, actor);
        return payments.findByBookingIdOrderByPaidAtAsc(booking.getId()).stream().map(PaymentResponse::from).toList();
    }

    /**
     * Records money received or refunded at the desk and updates the booking's running total.
     * Both rows are written in one transaction, and the booking's version column rejects a
     * concurrent payment on the same booking instead of losing one of them.
     */
    public BookingResponse record(UUID bookingId, RecordPaymentRequest request, AuthenticatedUser actor) {
        Booking booking = bookingService.findAccessible(bookingId, actor);
        PaymentType type = request.type() == null ? PaymentType.CHARGE : request.type();
        BigDecimal amount = PricingCalculator.money(request.amount());
        if (type == PaymentType.REFUND) {
            booking.recordRefund(amount);
        } else {
            booking.recordCharge(amount);
        }
        String reference = request.reference() == null || request.reference().isBlank()
                ? null
                : request.reference().trim();
        payments.save(new Payment(booking, type, request.method(), amount, reference, Instant.now(clock),
                users.getReferenceById(actor.id())));
        return bookingMapper.toResponse(booking);
    }
}
