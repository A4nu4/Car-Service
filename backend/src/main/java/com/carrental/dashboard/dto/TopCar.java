package com.carrental.dashboard.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record TopCar(UUID carId, String name, long rentals, BigDecimal revenue) {
}
