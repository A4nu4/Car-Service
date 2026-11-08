package com.carrental.dashboard;

import com.carrental.booking.BookingRepository;
import com.carrental.booking.BookingStatus;
import com.carrental.booking.PricingCalculator;
import com.carrental.car.CarRepository;
import com.carrental.car.CarStatus;
import com.carrental.common.exception.ApiException;
import com.carrental.config.RentalProperties;
import com.carrental.dashboard.dto.DashboardSummary;
import com.carrental.dashboard.dto.RevenuePoint;
import com.carrental.dashboard.dto.TopCar;
import com.carrental.payment.PaymentRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Read-only statistics. Calendar boundaries ("today", "this month") use the rental time zone. */
@Service
@Transactional(readOnly = true)
public class DashboardService {

    private static final long MAX_RANGE_DAYS = 366;
    private static final Set<BookingStatus> FINISHED =
            Set.of(BookingStatus.COMPLETED, BookingStatus.CANCELLED, BookingStatus.NO_SHOW);

    private final CarRepository cars;
    private final BookingRepository bookings;
    private final PaymentRepository payments;
    private final RentalProperties rental;
    private final Clock clock;

    public DashboardService(CarRepository cars, BookingRepository bookings, PaymentRepository payments,
                            RentalProperties rental, Clock clock) {
        this.cars = cars;
        this.bookings = bookings;
        this.payments = payments;
        this.rental = rental;
        this.clock = clock;
    }

    public DashboardSummary summary() {
        Instant now = Instant.now(clock);
        ZoneId zone = rental.timeZone();
        LocalDate today = LocalDate.ofInstant(now, zone);
        Instant startOfToday = today.atStartOfDay(zone).toInstant();
        Instant startOfTomorrow = today.plusDays(1).atStartOfDay(zone).toInstant();
        Instant startOfMonth = today.withDayOfMonth(1).atStartOfDay(zone).toInstant();

        Map<CarStatus, Long> byStatus = new EnumMap<>(CarStatus.class);
        cars.countByStatus().forEach(row -> byStatus.put(row.getStatus(), row.getCount()));
        long totalCars = byStatus.values().stream().mapToLong(Long::longValue).sum();
        long active = bookings.countByStatus(BookingStatus.ACTIVE);

        return new DashboardSummary(
                now,
                new DashboardSummary.Fleet(totalCars,
                        byStatus.getOrDefault(CarStatus.AVAILABLE, 0L),
                        byStatus.getOrDefault(CarStatus.MAINTENANCE, 0L),
                        byStatus.getOrDefault(CarStatus.RETIRED, 0L),
                        active),
                new DashboardSummary.Today(
                        bookings.countByStatusAndPickupAtGreaterThanEqualAndPickupAtLessThan(
                                BookingStatus.CONFIRMED, startOfToday, startOfTomorrow),
                        bookings.countByStatusAndDropoffAtGreaterThanEqualAndDropoffAtLessThan(
                                BookingStatus.ACTIVE, startOfToday, startOfTomorrow),
                        bookings.countByStatusAndDropoffAtLessThan(BookingStatus.ACTIVE, now)),
                new DashboardSummary.Bookings(bookings.countByStatus(BookingStatus.CONFIRMED), active),
                new DashboardSummary.Finance(
                        rental.currency(),
                        PricingCalculator.money(payments.sumNetRevenue(startOfMonth, now)),
                        PricingCalculator.money(bookings.sumOutstandingBalance(FINISHED))));
    }

    /** Net revenue per day or month for the inclusive date range [from, to]. */
    public List<RevenuePoint> revenue(LocalDate from, LocalDate to, Granularity granularity) {
        validateRange(from, to);
        ZoneId zone = rental.timeZone();
        return payments.sumNetRevenueBy(granularity.postgresUnit(), zone.getId(),
                        from.atStartOfDay(zone).toInstant(), to.plusDays(1).atStartOfDay(zone).toInstant())
                .stream()
                .map(row -> new RevenuePoint(LocalDate.parse(row.getPeriod()), PricingCalculator.money(row.getAmount())))
                .toList();
    }

    /** Most-rented cars among rentals completed in [from, to]. */
    public List<TopCar> topCars(LocalDate from, LocalDate to, int limit) {
        validateRange(from, to);
        ZoneId zone = rental.timeZone();
        return bookings.findTopCars(BookingStatus.COMPLETED, from.atStartOfDay(zone).toInstant(),
                        to.plusDays(1).atStartOfDay(zone).toInstant(), PageRequest.of(0, limit))
                .stream()
                .map(row -> new TopCar(row.getCarId(), row.getBrand() + " " + row.getModel(), row.getRentals(),
                        PricingCalculator.money(row.getRevenue())))
                .toList();
    }

    private static void validateRange(LocalDate from, LocalDate to) {
        if (to.isBefore(from)) {
            throw ApiException.businessRule("'to' must not be before 'from'");
        }
        if (ChronoUnit.DAYS.between(from, to) >= MAX_RANGE_DAYS) {
            throw ApiException.businessRule("The date range can span at most " + MAX_RANGE_DAYS + " days");
        }
    }
}
