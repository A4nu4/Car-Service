package com.carrental.booking;

import com.carrental.booking.dto.BookingFilter;
import com.carrental.booking.dto.BookingResponse;
import com.carrental.booking.dto.CancelBookingRequest;
import com.carrental.booking.dto.CreateBookingRequest;
import com.carrental.booking.dto.PickupRequest;
import com.carrental.booking.dto.RescheduleBookingRequest;
import com.carrental.booking.dto.ReturnRequest;
import com.carrental.common.web.PageResponse;
import com.carrental.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

/**
 * One set of endpoints for customers and staff. The service scopes results by caller:
 * customers see and change only their own bookings; staff see all.
 */
@RestController
@RequestMapping("/api/v1/bookings")
@Tag(name = "Bookings", description = "Reserve cars and manage the rental lifecycle")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @GetMapping
    @Operation(summary = "List bookings", description = "Customers get their own bookings; staff can filter all.")
    public PageResponse<BookingResponse> list(
            @ParameterObject BookingFilter filter,
            @ParameterObject @PageableDefault(sort = "pickupAt", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal AuthenticatedUser actor) {
        return PageResponse.of(bookingService.list(filter, actor, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a booking")
    @ApiResponse(responseCode = "404", description = "Not found (or not yours)")
    public BookingResponse get(@PathVariable UUID id, @AuthenticationPrincipal AuthenticatedUser actor) {
        return bookingService.get(id, actor);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('CUSTOMER', 'AGENT', 'ADMIN')")
    @Operation(summary = "Book a car",
            description = "Customers book for themselves (a complete driver profile is required). "
                    + "Staff must pass customerId. Pickup is at the car's current branch.")
    @ApiResponse(responseCode = "201", description = "Booking confirmed")
    @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR")
    @ApiResponse(responseCode = "404", description = "Car, customer or drop-off location not found")
    @ApiResponse(responseCode = "409", description = "CAR_UNAVAILABLE")
    @ApiResponse(responseCode = "422", description = "DRIVER_NOT_ELIGIBLE or BUSINESS_RULE_VIOLATION "
            + "(period in the past, too long, too far ahead...)")
    public ResponseEntity<BookingResponse> create(@Valid @RequestBody CreateBookingRequest request,
                                                  @AuthenticationPrincipal AuthenticatedUser actor) {
        BookingResponse booking = bookingService.create(request, actor);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(booking.id()).toUri();
        return ResponseEntity.created(location).body(booking);
    }

    @PostMapping("/{id}/reschedule")
    @Operation(summary = "Change pickup/drop-off times",
            description = "Customers: only until freeCancellationUntil. The agreed daily rate is kept.")
    @ApiResponse(responseCode = "409", description = "CAR_UNAVAILABLE or INVALID_STATE_TRANSITION")
    @ApiResponse(responseCode = "422", description = "Outside the free change window, or invalid period")
    public BookingResponse reschedule(@PathVariable UUID id, @Valid @RequestBody RescheduleBookingRequest request,
                                      @AuthenticationPrincipal AuthenticatedUser actor) {
        return bookingService.reschedule(id, request, actor);
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel a booking",
            description = "Customers: free, until freeCancellationUntil. Staff: any time before pickup; "
                    + "after that moment one day's rate is charged unless waiveFee is true. Body is optional.")
    @ApiResponse(responseCode = "409", description = "INVALID_STATE_TRANSITION (not CONFIRMED)")
    @ApiResponse(responseCode = "422", description = "Outside the free cancellation window")
    public BookingResponse cancel(@PathVariable UUID id,
                                  @Valid @RequestBody(required = false) CancelBookingRequest request,
                                  @AuthenticationPrincipal AuthenticatedUser actor) {
        CancelBookingRequest body = request != null ? request : new CancelBookingRequest(null, false);
        return bookingService.cancel(id, body, actor);
    }

    @PostMapping("/{id}/pickup")
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
    @Operation(summary = "Hand the car over (staff)", description = "CONFIRMED -> ACTIVE. Records the odometer.")
    @ApiResponse(responseCode = "409", description = "CAR_UNAVAILABLE or INVALID_STATE_TRANSITION")
    @ApiResponse(responseCode = "422", description = "Too early, period over, or mileage too low")
    public BookingResponse pickup(@PathVariable UUID id, @Valid @RequestBody PickupRequest request,
                                  @AuthenticationPrincipal AuthenticatedUser actor) {
        return bookingService.pickup(id, request, actor);
    }

    @PostMapping("/{id}/return")
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
    @Operation(summary = "Take the car back (staff)",
            description = "ACTIVE -> COMPLETED. Late fees are calculated automatically; the car is moved "
                    + "to the drop-off branch.")
    @ApiResponse(responseCode = "409", description = "INVALID_STATE_TRANSITION (not ACTIVE)")
    @ApiResponse(responseCode = "422", description = "Return mileage below pickup mileage")
    public BookingResponse returnCar(@PathVariable UUID id, @Valid @RequestBody ReturnRequest request,
                                     @AuthenticationPrincipal AuthenticatedUser actor) {
        return bookingService.returnCar(id, request, actor);
    }

    @PostMapping("/{id}/no-show")
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
    @Operation(summary = "Mark as no-show (staff)", description = "Charges one day's rate.")
    @ApiResponse(responseCode = "409", description = "INVALID_STATE_TRANSITION (not CONFIRMED)")
    @ApiResponse(responseCode = "422", description = "Pickup time has not passed yet")
    public BookingResponse noShow(@PathVariable UUID id, @AuthenticationPrincipal AuthenticatedUser actor) {
        return bookingService.markNoShow(id, actor);
    }
}
