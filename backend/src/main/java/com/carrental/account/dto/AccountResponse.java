package com.carrental.account.dto;

import com.carrental.customer.dto.DriverProfileResponse;
import com.carrental.user.dto.UserResponse;

/**
 * @param driverProfile present for customers, null for staff accounts
 */
public record AccountResponse(UserResponse user, DriverProfileResponse driverProfile) {
}
