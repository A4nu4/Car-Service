package com.carrental.payment;

import com.carrental.booking.dto.BookingResponse;
import com.carrental.payment.dto.PaymentResponse;
import com.carrental.payment.dto.RecordPaymentRequest;
import com.carrental.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/bookings/{bookingId}/payments")
@Tag(name = "Payments", description = "Payment ledger of a booking")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping
    @Operation(summary = "List payments of a booking", description = "Customers can view their own bookings' payments.")
    @ApiResponse(responseCode = "404", description = "Booking not found (or not yours)")
    public List<PaymentResponse> list(@PathVariable UUID bookingId,
                                      @AuthenticationPrincipal AuthenticatedUser actor) {
        return paymentService.list(bookingId, actor);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
    @Operation(summary = "Record a payment or refund (staff)",
            description = "Returns the updated booking, including amountPaid, balanceDue and paymentStatus.")
    @ApiResponse(responseCode = "201", description = "Recorded")
    @ApiResponse(responseCode = "409", description = "CONCURRENT_MODIFICATION: retry")
    @ApiResponse(responseCode = "422", description = "Refund exceeds the amount paid")
    public BookingResponse record(@PathVariable UUID bookingId, @Valid @RequestBody RecordPaymentRequest request,
                                  @AuthenticationPrincipal AuthenticatedUser actor) {
        return paymentService.record(bookingId, request, actor);
    }
}
