package com.carrental.dashboard;

public enum Granularity {
    DAY("day"),
    MONTH("month");

    private final String postgresUnit;

    Granularity(String postgresUnit) {
        this.postgresUnit = postgresUnit;
    }

    /** Value for PostgreSQL date_trunc. Coming from an enum, it can never be attacker-controlled. */
    String postgresUnit() {
        return postgresUnit;
    }
}
