package com.carrental.user.dto;

import com.carrental.user.Role;
import com.carrental.user.User;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String firstName,
        String lastName,
        String phone,
        Role role,
        boolean enabled,
        Instant lastLoginAt,
        Instant createdAt
) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhone(),
                user.getRole(),
                user.isEnabled(),
                user.getLastLoginAt(),
                user.getCreatedAt());
    }
}
