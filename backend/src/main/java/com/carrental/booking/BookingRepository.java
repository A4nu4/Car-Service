package com.carrental.booking;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BookingRepository extends JpaRepository<Booking, UUID>, JpaSpecificationExecutor<Booking> {

    @Override
    @EntityGraph(attributePaths = {"car", "pickupLocation", "dropoffLocation", "customer"})
    Page<Booking> findAll(Specification<Booking> spec, Pageable pageable);

    @EntityGraph(attributePaths = {"car", "car.location", "pickupLocation", "dropoffLocation", "customer"})
    Optional<Booking> findWithDetailsById(UUID id);

    @Query("""
            select count(b) > 0 from Booking b
            where b.car.id = :carId and b.status in :statuses
              and b.pickupAt < :to and b.dropoffAt > :from
            """)
    boolean existsOverlapping(@Param("carId") UUID carId, @Param("from") Instant from, @Param("to") Instant to,
                              @Param("statuses") Collection<BookingStatus> statuses);

    @Query("""
            select count(b) > 0 from Booking b
            where b.car.id = :carId and b.status in :statuses
              and b.pickupAt < :to and b.dropoffAt > :from
              and b.id <> :excludedId
            """)
    boolean existsOverlappingExcluding(@Param("carId") UUID carId, @Param("from") Instant from,
                                       @Param("to") Instant to,
                                       @Param("statuses") Collection<BookingStatus> statuses,
                                       @Param("excludedId") UUID excludedId);

    boolean existsByCarIdAndStatusIn(UUID carId, Collection<BookingStatus> statuses);

    boolean existsByCarIdAndStatusAndIdNot(UUID carId, BookingStatus status, UUID id);

    // ----- dashboard queries -----

    long countByStatus(BookingStatus status);

    long countByStatusAndPickupAtGreaterThanEqualAndPickupAtLessThan(BookingStatus status, Instant from, Instant to);

    long countByStatusAndDropoffAtGreaterThanEqualAndDropoffAtLessThan(BookingStatus status, Instant from, Instant to);

    long countByStatusAndDropoffAtLessThan(BookingStatus status, Instant before);

    @Query("""
            select coalesce(sum(b.totalPrice - b.amountPaid), 0) from Booking b
            where b.status in :statuses and b.totalPrice > b.amountPaid
            """)
    BigDecimal sumOutstandingBalance(@Param("statuses") Collection<BookingStatus> statuses);

    @Query("""
            select c.id as carId, c.brand as brand, c.model as model,
                   count(b) as rentals, coalesce(sum(b.totalPrice), 0) as revenue
            from Booking b join b.car c
            where b.status = :status and b.actualReturnAt >= :from and b.actualReturnAt < :to
            group by c.id, c.brand, c.model
            order by count(b) desc, coalesce(sum(b.totalPrice), 0) desc
            """)
    List<CarPerformance> findTopCars(@Param("status") BookingStatus status, @Param("from") Instant from,
                                     @Param("to") Instant to, Pageable limit);

    interface CarPerformance {
        UUID getCarId();

        String getBrand();

        String getModel();

        long getRentals();

        BigDecimal getRevenue();
    }
}
