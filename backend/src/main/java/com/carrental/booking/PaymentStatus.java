package com.carrental.booking;

/** Derived from the booking total and the amount paid; never stored. */
public enum PaymentStatus {
    UNPAID,
    PARTIALLY_PAID,
    PAID,
    /** More was paid than is owed (e.g. after a free cancellation): a refund should be issued. */
    REFUND_DUE
}
