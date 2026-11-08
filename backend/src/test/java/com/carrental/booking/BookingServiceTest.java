package com.carrental.booking;

import com.carrental.booking.dto.BookingResponse;
import com.carrental.booking.dto.CancelBookingRequest;
import com.carrental.booking.dto.CreateBookingRequest;
import com.carrental.car.Car;
import com.carrental.car.CarRepository;
import com.carrental.car.CarStatus;
import com.carrental.common.exception.ApiException;
import com.carrental.common.exception.ErrorCode;
import com.carrental.config.RentalProperties;
import com.carrental.customer.CustomerProfile;
import com.carrental.customer.CustomerService;
import com.carrental.customer.DriverEligibilityChecker;
import com.carrental.location.LocationService;
import com.carrental.security.AuthenticatedUser;
import com.carrental.support.MutableClock;
import com.carrental.support.TestFixtures;
import com.carrental.user.Role;
import com.carrental.user.User;
import com.carrental.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-02T08:00:00Z");
    private static final Instant PICKUP = Instant.parse("2026-11-10T09:00:00Z");
    private static final Instant DROPOFF = Instant.parse("2026-11-12T09:00:00Z");

    @Mock
    private BookingRepository bookings;
    @Mock
    private CarRepository cars;
    @Mock
    private UserRepository users;
    @Mock
    private LocationService locationService;
    @Mock
    private CustomerService customerService;
    @Mock
    private DriverEligibilityChecker eligibility;

    private final MutableClock clock = new MutableClock(NOW);
    private BookingService service;
    private User customer;
    private AuthenticatedUser customerPrincipal;
    private Car car;

    @BeforeEach
    void setUp() {
        RentalProperties rental = TestFixtures.rentalProperties();
        service = new BookingService(bookings, cars, users, locationService, customerService, eligibility,
                new PricingCalculator(rental), new BookingReferenceGenerator(), new BookingMapper(rental), rental, clock);
        customer = TestFixtures.user(Role.CUSTOMER);
        customerPrincipal = new AuthenticatedUser(customer.getId(), customer.getEmail(), Role.CUSTOMER);
        car = TestFixtures.car("50.00");
    }

    @Test
    void createsBookingWithPriceSnapshot() {
        when(users.findById(customer.getId())).thenReturn(Optional.of(customer));
        when(cars.findByIdForUpdate(car.getId())).thenReturn(Optional.of(car));
        when(customerService.findProfile(customer.getId())).thenReturn(new CustomerProfile(customer));
        when(bookings.existsOverlapping(eq(car.getId()), eq(PICKUP), eq(DROPOFF), any())).thenReturn(false);
        when(bookings.saveAndFlush(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BookingResponse response = service.create(request(), customerPrincipal);

        assertThat(response.status()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(response.price().rentalDays()).isEqualTo(2);
        assertThat(response.price().totalPrice()).isEqualByComparingTo("100.00");
        assertThat(response.reference()).startsWith("BK-");
    }

    @Test
    void rejectsOverlappingBookingWithoutSaving() {
        when(users.findById(customer.getId())).thenReturn(Optional.of(customer));
        when(cars.findByIdForUpdate(car.getId())).thenReturn(Optional.of(car));
        when(customerService.findProfile(customer.getId())).thenReturn(new CustomerProfile(customer));
        when(bookings.existsOverlapping(eq(car.getId()), eq(PICKUP), eq(DROPOFF), any())).thenReturn(true);

        assertThatThrownBy(() -> service.create(request(), customerPrincipal))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getCode())
                .isEqualTo(ErrorCode.CAR_UNAVAILABLE);
        verify(bookings, never()).saveAndFlush(any());
    }

    @Test
    void rejectsCarInMaintenance() {
        car.changeStatus(CarStatus.MAINTENANCE);
        when(users.findById(customer.getId())).thenReturn(Optional.of(customer));
        when(cars.findByIdForUpdate(car.getId())).thenReturn(Optional.of(car));

        assertThatThrownBy(() -> service.create(request(), customerPrincipal))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getCode())
                .isEqualTo(ErrorCode.CAR_UNAVAILABLE);
    }

    @Test
    void rejectsPickupInThePast() {
        when(users.findById(customer.getId())).thenReturn(Optional.of(customer));
        CreateBookingRequest past = new CreateBookingRequest(car.getId(), NOW.minusSeconds(3600), DROPOFF,
                null, null, null);

        assertThatThrownBy(() -> service.create(past, customerPrincipal))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getCode())
                .isEqualTo(ErrorCode.BUSINESS_RULE_VIOLATION);
    }

    @Test
    void rejectsRentalLongerThanMaximum() {
        when(users.findById(customer.getId())).thenReturn(Optional.of(customer));
        CreateBookingRequest tooLong = new CreateBookingRequest(car.getId(), PICKUP,
                PICKUP.plusSeconds(31L * 24 * 3600), null, null, null);

        assertThatThrownBy(() -> service.create(tooLong, customerPrincipal))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("at most 30 days");
    }

    @Test
    void staffMustNameTheCustomer() {
        AuthenticatedUser agent = new AuthenticatedUser(UUID.randomUUID(), "agent@example.com", Role.AGENT);

        assertThatThrownBy(() -> service.create(request(), agent))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("customerId is required");
    }

    @Test
    void staffLateCancellationChargesOneDayUnlessWaived() {
        Booking late = existingBooking();
        when(bookings.findWithDetailsById(late.getId())).thenReturn(Optional.of(late));
        clock.set(PICKUP.minusSeconds(3600));
        AuthenticatedUser agent = new AuthenticatedUser(UUID.randomUUID(), "agent@example.com", Role.AGENT);

        BookingResponse response = service.cancel(late.getId(), new CancelBookingRequest(null, false), agent);

        assertThat(response.price().cancellationFee()).isEqualByComparingTo("50.00");

        Booking waived = existingBooking();
        when(bookings.findWithDetailsById(waived.getId())).thenReturn(Optional.of(waived));
        assertThat(service.cancel(waived.getId(), new CancelBookingRequest(null, true), agent)
                .price().cancellationFee()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void customerCannotCancelInsideWindow() {
        Booking booking = existingBooking();
        when(bookings.findWithDetailsById(booking.getId())).thenReturn(Optional.of(booking));
        clock.set(PICKUP.minusSeconds(3600));

        assertThatThrownBy(() -> service.cancel(booking.getId(), new CancelBookingRequest(null, false),
                customerPrincipal))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getCode())
                .isEqualTo(ErrorCode.BUSINESS_RULE_VIOLATION);
    }

    @Test
    void otherCustomersBookingIsNotFound() {
        Booking booking = existingBooking();
        when(bookings.findWithDetailsById(booking.getId())).thenReturn(Optional.of(booking));
        AuthenticatedUser stranger = new AuthenticatedUser(UUID.randomUUID(), "x@example.com", Role.CUSTOMER);

        assertThatThrownBy(() -> service.get(booking.getId(), stranger))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getCode())
                .isEqualTo(ErrorCode.NOT_FOUND);
    }

    private CreateBookingRequest request() {
        return new CreateBookingRequest(car.getId(), PICKUP, DROPOFF, null, null, null);
    }

    private Booking existingBooking() {
        PriceQuote quote = new PriceQuote(new BigDecimal("50.00"), 2, new BigDecimal("100.00"));
        return TestFixtures.withId(new Booking("BK-" + UUID.randomUUID().toString().substring(0, 8), customer, car,
                car.getLocation(), PICKUP, DROPOFF, quote, null, customer));
    }
}
