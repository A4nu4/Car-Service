package com.carrental.payment;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    @EntityGraph(attributePaths = "recordedBy")
    List<Payment> findByBookingIdOrderByPaidAtAsc(UUID bookingId);

    /** Net revenue (charges minus refunds) received in [from, to). */
    @Query("""
            select coalesce(sum(case when p.type = com.carrental.payment.PaymentType.CHARGE
                                     then p.amount else -p.amount end), 0)
            from Payment p where p.paidAt >= :from and p.paidAt < :to
            """)
    BigDecimal sumNetRevenue(@Param("from") Instant from, @Param("to") Instant to);

    /**
     * Net revenue per calendar day or month in the given time zone.
     *
     * @param unit 'day' or 'month' (validated by the caller; passed to date_trunc)
     */
    @Query(value = """
            select to_char(date_trunc(:unit, p.paid_at at time zone :zone), 'YYYY-MM-DD') as period,
                   sum(case when p.type = 'CHARGE' then p.amount else -p.amount end) as amount
            from payments p
            where p.paid_at >= :from and p.paid_at < :to
            group by 1
            order by 1
            """, nativeQuery = true)
    List<RevenueRow> sumNetRevenueBy(@Param("unit") String unit, @Param("zone") String zone,
                                     @Param("from") Instant from, @Param("to") Instant to);

    interface RevenueRow {
        String getPeriod();

        BigDecimal getAmount();
    }
}
