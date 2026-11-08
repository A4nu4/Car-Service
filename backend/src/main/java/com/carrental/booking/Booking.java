package com.carrental.booking;

import com.carrental.car.Car;
import com.carrental.common.entity.BaseEntity;
import com.carrental.common.exception.ApiException;
import com.carrental.location.Location;
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
import java.util.UUID;

/**
 * A reservation of one car for one period. State changes go through the methods below, which
 * reject illegal transitions, so the lifecycle rules live in exactly one place.
 * Prices are copied onto the booking when it is made: later price changes never alter it.
 */
@Entity
@Table(name = "bookings")
public class Booking extends BaseEntity {

    @Column(nullable = false, unique = true, updatable = false)
    private String reference;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false, updatable = false)
    private User customer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "car_id", nullable = false, updatable = false)
    private Car car;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pickup_location_id", nullable = false, updatable = false)
    private Location pickupLocation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dropoff_location_id", nullable = false)
    private Location dropoffLocation;

    @Column(name = "pickup_at", nullable = false)
    private Instant pickupAt;

    @Column(name = "dropoff_at", nullable = false)
    private Instant dropoffAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status;

    @Column(name = "daily_rate", nullable = false, precision = 10, scale = 2)
    private BigDecimal dailyRate;

    @Column(name = "rental_days", nullable = false)
    private int rentalDays;

    @Column(name = "base_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal basePrice;

    @Column(name = "late_fee", nullable = false, precision = 12, scale = 2)
    private BigDecimal lateFee = BigDecimal.ZERO;

    @Column(name = "extra_charges", nullable = false, precision = 12, scale = 2)
    private BigDecimal extraCharges = BigDecimal.ZERO;

    @Column(name = "cancellation_fee", nullable = false, precision = 12, scale = 2)
    private BigDecimal cancellationFee = BigDecimal.ZERO;

    @Column(name = "total_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalPrice;

    @Column(name = "amount_paid", nullable = false, precision = 12, scale = 2)
    private BigDecimal amountPaid = BigDecimal.ZERO;

    @Column(name = "customer_notes")
    private String customerNotes;

    @Column(name = "actual_pickup_at")
    private Instant actualPickupAt;

    @Column(name = "actual_return_at")
    private Instant actualReturnAt;

    @Column(name = "pickup_mileage")
    private Integer pickupMileage;

    @Column(name = "return_mileage")
    private Integer returnMileage;

    @Column(name = "return_notes")
    private String returnNotes;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "cancellation_reason")
    private String cancellationReason;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false, updatable = false)
    private User createdBy;

    protected Booking() {
        // for JPA
    }

    public Booking(String reference, User customer, Car car, Location dropoffLocation, Instant pickupAt,
                   Instant dropoffAt, PriceQuote quote, String customerNotes, User createdBy) {
        this.reference = reference;
        this.customer = customer;
        this.car = car;
        this.pickupLocation = car.getLocation();
        this.dropoffLocation = dropoffLocation;
        this.customerNotes = customerNotes;
        this.createdBy = createdBy;
        this.status = BookingStatus.CONFIRMED;
        applySchedule(pickupAt, dropoffAt, quote);
    }

    public void reschedule(Instant newPickupAt, Instant newDropoffAt, PriceQuote quote) {
        requireStatus(BookingStatus.CONFIRMED, "reschedule");
        applySchedule(newPickupAt, newDropoffAt, quote);
    }

    public void cancel(Instant now, String reason, BigDecimal fee) {
        requireStatus(BookingStatus.CONFIRMED, "cancel");
        this.status = BookingStatus.CANCELLED;
        this.cancelledAt = now;
        this.cancellationReason = reason;
        this.cancellationFee = fee;
        recalculateTotal();
    }

    public void markNoShow(Instant now, BigDecimal fee) {
        requireStatus(BookingStatus.CONFIRMED, "mark as no-show");
        this.status = BookingStatus.NO_SHOW;
        this.cancelledAt = now;
        this.cancellationReason = "Customer did not show up";
        this.cancellationFee = fee;
        recalculateTotal();
    }

    public void startRental(Instant now, int mileage) {
        requireStatus(BookingStatus.CONFIRMED, "hand over");
        this.status = BookingStatus.ACTIVE;
        this.actualPickupAt = now;
        this.pickupMileage = mileage;
    }

    public void completeRental(Instant now, int mileage, BigDecimal lateFee, BigDecimal extraCharges, String notes) {
        requireStatus(BookingStatus.ACTIVE, "return");
        if (mileage < pickupMileage) {
            throw ApiException.businessRule("Return mileage (" + mileage + ") is lower than pickup mileage ("
                    + pickupMileage + ")");
        }
        this.status = BookingStatus.COMPLETED;
        this.actualReturnAt = now;
        this.returnMileage = mileage;
        this.lateFee = lateFee;
        this.extraCharges = extraCharges;
        this.returnNotes = notes;
        recalculateTotal();
    }

    public void recordCharge(BigDecimal amount) {
        this.amountPaid = PricingCalculator.money(amountPaid.add(amount));
    }

    public void recordRefund(BigDecimal amount) {
        if (amount.compareTo(amountPaid) > 0) {
            throw ApiException.businessRule("Cannot refund more than the " + amountPaid + " paid so far");
        }
        this.amountPaid = PricingCalculator.money(amountPaid.subtract(amount));
    }

    public PaymentStatus getPaymentStatus() {
        int comparison = amountPaid.compareTo(totalPrice);
        if (comparison > 0) {
            return PaymentStatus.REFUND_DUE;
        }
        if (comparison == 0) {
            return PaymentStatus.PAID;
        }
        return amountPaid.signum() == 0 ? PaymentStatus.UNPAID : PaymentStatus.PARTIALLY_PAID;
    }

    public BigDecimal getBalanceDue() {
        return totalPrice.subtract(amountPaid);
    }

    public boolean isOwnedBy(UUID userId) {
        return customer.getId().equals(userId);
    }

    private void applySchedule(Instant newPickupAt, Instant newDropoffAt, PriceQuote quote) {
        this.pickupAt = newPickupAt;
        this.dropoffAt = newDropoffAt;
        this.dailyRate = quote.dailyRate();
        this.rentalDays = quote.rentalDays();
        this.basePrice = quote.basePrice();
        recalculateTotal();
    }

    private void recalculateTotal() {
        if (status == BookingStatus.CANCELLED || status == BookingStatus.NO_SHOW) {
            this.totalPrice = cancellationFee;
        } else {
            this.totalPrice = PricingCalculator.money(basePrice.add(lateFee).add(extraCharges));
        }
    }

    private void requireStatus(BookingStatus required, String action) {
        if (status != required) {
            throw ApiException.invalidTransition(
                    "Cannot " + action + " a booking with status " + status + " (must be " + required + ")");
        }
    }

    public String getReference() {
        return reference;
    }

    public User getCustomer() {
        return customer;
    }

    public Car getCar() {
        return car;
    }

    public Location getPickupLocation() {
        return pickupLocation;
    }

    public Location getDropoffLocation() {
        return dropoffLocation;
    }

    public Instant getPickupAt() {
        return pickupAt;
    }

    public Instant getDropoffAt() {
        return dropoffAt;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public BigDecimal getDailyRate() {
        return dailyRate;
    }

    public int getRentalDays() {
        return rentalDays;
    }

    public BigDecimal getBasePrice() {
        return basePrice;
    }

    public BigDecimal getLateFee() {
        return lateFee;
    }

    public BigDecimal getExtraCharges() {
        return extraCharges;
    }

    public BigDecimal getCancellationFee() {
        return cancellationFee;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public BigDecimal getAmountPaid() {
        return amountPaid;
    }

    public String getCustomerNotes() {
        return customerNotes;
    }

    public Instant getActualPickupAt() {
        return actualPickupAt;
    }

    public Instant getActualReturnAt() {
        return actualReturnAt;
    }

    public Integer getPickupMileage() {
        return pickupMileage;
    }

    public Integer getReturnMileage() {
        return returnMileage;
    }

    public String getReturnNotes() {
        return returnNotes;
    }

    public Instant getCancelledAt() {
        return cancelledAt;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public User getCreatedBy() {
        return createdBy;
    }
}
