package com.carrental.booking;

import com.carrental.booking.dto.BookingFilter;
import com.carrental.booking.dto.BookingResponse;
import com.carrental.booking.dto.CancelBookingRequest;
import com.carrental.booking.dto.CreateBookingRequest;
import com.carrental.booking.dto.PickupRequest;
import com.carrental.booking.dto.RescheduleBookingRequest;
import com.carrental.booking.dto.ReturnRequest;
import com.carrental.car.Car;
import com.carrental.car.CarRepository;
import com.carrental.car.CarStatus;
import com.carrental.common.exception.ApiException;
import com.carrental.common.exception.ErrorCode;
import com.carrental.config.RentalProperties;
import com.carrental.customer.CustomerService;
import com.carrental.customer.DriverEligibilityChecker;
import com.carrental.location.Location;
import com.carrental.location.LocationService;
import com.carrental.security.AuthenticatedUser;
import com.carrental.user.Role;
import com.carrental.user.User;
import com.carrental.user.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * The booking lifecycle. Double-booking is prevented in three layers:
 * <ol>
 *   <li>the car row is locked (SELECT ... FOR UPDATE) so concurrent bookings of one car run one after another;</li>
 *   <li>an explicit overlap check gives a clear error message;</li>
 *   <li>the database exclusion constraint is the final guarantee if anything above is ever bypassed.</li>
 * </ol>
 */
@Service
@Transactional
public class BookingService {

    /** Tolerates clock skew and slow forms: a pickup time a few minutes in the past is still accepted. */
    private static final Duration PAST_PICKUP_TOLERANCE = Duration.ofMinutes(5);
    private static final Duration EARLY_PICKUP_ALLOWANCE = Duration.ofHours(2);

    private final BookingRepository bookings;
    private final CarRepository cars;
    private final UserRepository users;
    private final LocationService locationService;
    private final CustomerService customerService;
    private final DriverEligibilityChecker eligibility;
    private final PricingCalculator pricing;
    private final BookingReferenceGenerator referenceGenerator;
    private final BookingMapper mapper;
    private final RentalProperties rental;
    private final Clock clock;

    public BookingService(BookingRepository bookings, CarRepository cars, UserRepository users,
                          LocationService locationService, CustomerService customerService,
                          DriverEligibilityChecker eligibility, PricingCalculator pricing,
                          BookingReferenceGenerator referenceGenerator, BookingMapper mapper,
                          RentalProperties rental, Clock clock) {
        this.bookings = bookings;
        this.cars = cars;
        this.users = users;
        this.locationService = locationService;
        this.customerService = customerService;
        this.eligibility = eligibility;
        this.pricing = pricing;
        this.referenceGenerator = referenceGenerator;
        this.mapper = mapper;
        this.rental = rental;
        this.clock = clock;
    }

    // ------------------------------------------------------------------ queries

    @Transactional(readOnly = true)
    public Page<BookingResponse> list(BookingFilter filter, AuthenticatedUser actor, Pageable pageable) {
        Specification<Booking> spec = actor.isStaff()
                ? Specification.allOf(
                        BookingSpecifications.hasStatus(filter.status()),
                        BookingSpecifications.forCustomer(filter.customerId()),
                        BookingSpecifications.forCar(filter.carId()),
                        BookingSpecifications.pickupAtLocation(filter.locationId()),
                        BookingSpecifications.pickupFrom(filter.pickupFrom()),
                        BookingSpecifications.pickupBefore(filter.pickupTo()),
                        BookingSpecifications.hasReference(filter.reference()))
                : Specification.allOf(
                        BookingSpecifications.forCustomer(actor.id()),
                        BookingSpecifications.hasStatus(filter.status()),
                        BookingSpecifications.pickupFrom(filter.pickupFrom()),
                        BookingSpecifications.pickupBefore(filter.pickupTo()),
                        BookingSpecifications.hasReference(filter.reference()));
        return bookings.findAll(spec, pageable).map(mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public BookingResponse get(UUID id, AuthenticatedUser actor) {
        return mapper.toResponse(findAccessible(id, actor));
    }

    /**
     * Customers only see their own bookings. Someone else's booking is reported as "not found"
     * rather than "forbidden", so ids cannot be probed for existence.
     */
    @Transactional(readOnly = true)
    public Booking findAccessible(UUID id, AuthenticatedUser actor) {
        return bookings.findWithDetailsById(id)
                .filter(booking -> actor.isStaff() || booking.isOwnedBy(actor.id()))
                .orElseThrow(() -> ApiException.notFound("Booking", id));
    }

    // ------------------------------------------------------------------ commands

    public BookingResponse create(CreateBookingRequest request, AuthenticatedUser actor) {
        Instant now = Instant.now(clock);
        User customer = resolveCustomer(request.customerId(), actor);
        validatePeriod(request.pickupAt(), request.dropoffAt(), now);

        Car car = cars.findByIdForUpdate(request.carId())
                .orElseThrow(() -> ApiException.notFound("Car", request.carId()));
        if (!car.isRentable()) {
            throw new ApiException(ErrorCode.CAR_UNAVAILABLE, "This car is currently not available for rental");
        }
        Location dropoffLocation = resolveDropoffLocation(request.dropoffLocationId(), car);
        eligibility.check(customerService.findProfile(customer.getId()), request.pickupAt(), request.dropoffAt());

        if (bookings.existsOverlapping(car.getId(), request.pickupAt(), request.dropoffAt(), BookingStatus.BLOCKING)) {
            throw carAlreadyBooked();
        }
        PriceQuote quote = pricing.quote(car.getPricePerDay(), request.pickupAt(), request.dropoffAt());
        User createdBy = actor.id().equals(customer.getId()) ? customer : users.getReferenceById(actor.id());
        Booking booking = new Booking(referenceGenerator.next(), customer, car, dropoffLocation, request.pickupAt(),
                request.dropoffAt(), quote, trimToNull(request.notes()), createdBy);
        saveGuardingOverlap(booking);
        return mapper.toResponse(booking);
    }

    public BookingResponse reschedule(UUID id, RescheduleBookingRequest request, AuthenticatedUser actor) {
        Instant now = Instant.now(clock);
        Booking booking = findAccessible(id, actor);
        if (!actor.isStaff() && booking.getStatus() == BookingStatus.CONFIRMED) {
            requireFreeChangeWindow(booking, now, "reschedule");
        }
        validatePeriod(request.pickupAt(), request.dropoffAt(), now);
        Car car = cars.findByIdForUpdate(booking.getCar().getId())
                .orElseThrow(() -> ApiException.notFound("Car", booking.getCar().getId()));
        eligibility.check(customerService.findProfile(booking.getCustomer().getId()),
                request.pickupAt(), request.dropoffAt());
        if (bookings.existsOverlappingExcluding(car.getId(), request.pickupAt(), request.dropoffAt(),
                BookingStatus.BLOCKING, booking.getId())) {
            throw carAlreadyBooked();
        }
        // The rate agreed at booking time is kept; only the number of days changes.
        PriceQuote quote = pricing.quote(booking.getDailyRate(), request.pickupAt(), request.dropoffAt());
        booking.reschedule(request.pickupAt(), request.dropoffAt(), quote);
        saveGuardingOverlap(booking);
        return mapper.toResponse(booking);
    }

    public BookingResponse cancel(UUID id, CancelBookingRequest request, AuthenticatedUser actor) {
        Instant now = Instant.now(clock);
        Booking booking = findAccessible(id, actor);
        boolean free = isWithinFreeChangeWindow(booking, now);
        BigDecimal fee;
        if (actor.isStaff()) {
            fee = free || request.waiveFee()
                    ? PricingCalculator.money(BigDecimal.ZERO)
                    : pricing.lateCancellationFee(booking.getDailyRate());
        } else {
            if (booking.getStatus() == BookingStatus.CONFIRMED) {
                requireFreeChangeWindow(booking, now, "cancel");
            }
            fee = PricingCalculator.money(BigDecimal.ZERO);
        }
        booking.cancel(now, trimToNull(request.reason()), fee);
        return mapper.toResponse(booking);
    }

    /** Staff hand the keys over: the rental becomes ACTIVE. */
    public BookingResponse pickup(UUID id, PickupRequest request, AuthenticatedUser actor) {
        Instant now = Instant.now(clock);
        Booking booking = findAccessible(id, actor);
        if (now.isBefore(booking.getPickupAt().minus(EARLY_PICKUP_ALLOWANCE))) {
            throw ApiException.businessRule("Pickup is possible from " + EARLY_PICKUP_ALLOWANCE.toHours()
                    + " hours before the scheduled time");
        }
        if (!now.isBefore(booking.getDropoffAt())) {
            throw ApiException.businessRule("The rental period has already ended; reschedule it or mark it as no-show");
        }
        Car car = cars.findByIdForUpdate(booking.getCar().getId())
                .orElseThrow(() -> ApiException.notFound("Car", booking.getCar().getId()));
        if (car.getStatus() != CarStatus.AVAILABLE) {
            throw new ApiException(ErrorCode.CAR_UNAVAILABLE, "The car is currently " + car.getStatus());
        }
        if (bookings.existsByCarIdAndStatusAndIdNot(car.getId(), BookingStatus.ACTIVE, booking.getId())) {
            throw new ApiException(ErrorCode.CAR_UNAVAILABLE, "The car has not been returned from its previous rental");
        }
        if (request.mileage() < car.getMileage()) {
            throw ApiException.businessRule("Mileage cannot be lower than the car's recorded " + car.getMileage() + " km");
        }
        booking.startRental(now, request.mileage());
        car.recordMileage(request.mileage());
        return mapper.toResponse(booking);
    }

    /** Staff take the car back: late fees are computed, the car moves to the drop-off branch. */
    public BookingResponse returnCar(UUID id, ReturnRequest request, AuthenticatedUser actor) {
        Instant now = Instant.now(clock);
        Booking booking = findAccessible(id, actor);
        Car car = cars.findByIdForUpdate(booking.getCar().getId())
                .orElseThrow(() -> ApiException.notFound("Car", booking.getCar().getId()));
        BigDecimal lateFee = pricing.lateFee(booking.getDailyRate(), booking.getDropoffAt(), now);
        BigDecimal extras = PricingCalculator.money(
                request.extraCharges() == null ? BigDecimal.ZERO : request.extraCharges());
        booking.completeRental(now, request.mileage(), lateFee, extras, trimToNull(request.notes()));
        car.recordMileage(request.mileage());
        car.moveTo(booking.getDropoffLocation());
        return mapper.toResponse(booking);
    }

    public BookingResponse markNoShow(UUID id, AuthenticatedUser actor) {
        Instant now = Instant.now(clock);
        Booking booking = findAccessible(id, actor);
        if (now.isBefore(booking.getPickupAt())) {
            throw ApiException.businessRule("A booking can only be marked as no-show after its pickup time");
        }
        booking.markNoShow(now, pricing.lateCancellationFee(booking.getDailyRate()));
        return mapper.toResponse(booking);
    }

    // ------------------------------------------------------------------ helpers

    private User resolveCustomer(UUID requestedCustomerId, AuthenticatedUser actor) {
        if (!actor.isStaff()) {
            return users.findById(actor.id()).orElseThrow(() -> ApiException.notFound("User", actor.id()));
        }
        if (requestedCustomerId == null) {
            throw ApiException.businessRule("customerId is required when staff create a booking");
        }
        User customer = users.findById(requestedCustomerId)
                .filter(user -> user.getRole() == Role.CUSTOMER)
                .orElseThrow(() -> ApiException.notFound("Customer", requestedCustomerId));
        if (!customer.isEnabled()) {
            throw ApiException.businessRule("The customer's account is disabled");
        }
        return customer;
    }

    private Location resolveDropoffLocation(UUID dropoffLocationId, Car car) {
        if (dropoffLocationId == null) {
            return car.getLocation();
        }
        Location location = locationService.find(dropoffLocationId);
        if (!location.isActive()) {
            throw ApiException.businessRule("The drop-off location is not accepting returns");
        }
        return location;
    }

    private void validatePeriod(Instant pickupAt, Instant dropoffAt, Instant now) {
        if (!dropoffAt.isAfter(pickupAt)) {
            throw ApiException.businessRule("dropoffAt must be after pickupAt");
        }
        if (pickupAt.isBefore(now.minus(PAST_PICKUP_TOLERANCE))) {
            throw ApiException.businessRule("pickupAt must be in the future");
        }
        if (pickupAt.isAfter(now.plus(Duration.ofDays(rental.bookingHorizonDays())))) {
            throw ApiException.businessRule("Bookings can be made at most " + rental.bookingHorizonDays()
                    + " days in advance");
        }
        if (PricingCalculator.rentalDays(pickupAt, dropoffAt) > rental.maxRentalDays()) {
            throw ApiException.businessRule("A rental can last at most " + rental.maxRentalDays() + " days");
        }
    }

    private boolean isWithinFreeChangeWindow(Booking booking, Instant now) {
        return now.isBefore(booking.getPickupAt().minus(rental.freeCancellationWindow()));
    }

    private void requireFreeChangeWindow(Booking booking, Instant now, String action) {
        if (!isWithinFreeChangeWindow(booking, now)) {
            throw ApiException.businessRule("Bookings can only be changed online until "
                    + rental.freeCancellationWindow().toHours() + " hours before pickup. "
                    + "Please contact the rental desk to " + action + " this booking.");
        }
    }

    private void saveGuardingOverlap(Booking booking) {
        try {
            bookings.saveAndFlush(booking);
        } catch (DataIntegrityViolationException e) {
            throw carAlreadyBooked();
        }
    }

    private static ApiException carAlreadyBooked() {
        return new ApiException(ErrorCode.CAR_UNAVAILABLE, "The car is already booked for part of this period");
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
