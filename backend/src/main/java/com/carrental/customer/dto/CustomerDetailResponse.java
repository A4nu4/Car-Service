package com.carrental.customer.dto;

import com.carrental.user.dto.UserResponse;

public record CustomerDetailResponse(UserResponse account, DriverProfileResponse driverProfile) {
}
