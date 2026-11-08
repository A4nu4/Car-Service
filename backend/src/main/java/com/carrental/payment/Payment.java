package com.carrental.payment;

import com.carrental.booking.Booking;
import com.carrental.common.entity.BaseEntity;
import com.carrental.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * One money movement for a booking. Payments are never edited or deleted: a mistake is corrected
 * with a REFUND entry, so the ledger always shows what really happened.
 */
@Entity
@Table(name = "payments")
public class Payment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false, updatable = false)
    private Booking booking;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private PaymentType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private PaymentMethod method;

    @Column(nullable = false, updatable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(updatable = false)
    private String reference;

    @Column(name = "paid_at", nullable = false, updatable = false)
    private Instant paidAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recorded_by", nullable = false, updatable = false)
    private User recordedBy;

    protected Payment() {
        // for JPA
    }

    public Payment(Booking booking, PaymentType type, PaymentMethod method, BigDecimal amount, String reference,
                   Instant paidAt, User recordedBy) {
        this.booking = booking;
        this.type = type;
        this.method = method;
        this.amount = amount;
        this.reference = reference;
        this.paidAt = paidAt;
        this.recordedBy = recordedBy;
    }

    public Booking getBooking() {
        return booking;
    }

    public PaymentType getType() {
        return type;
    }

    public PaymentMethod getMethod() {
        return method;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getReference() {
        return reference;
    }

    public Instant getPaidAt() {
        return paidAt;
    }

    public User getRecordedBy() {
        return recordedBy;
    }
}
