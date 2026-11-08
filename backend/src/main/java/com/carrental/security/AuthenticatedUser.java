package com.carrental.security;

import com.carrental.user.Role;

import java.util.UUID;

/**
 * The caller of the current request, built from the verified JWT. Inject it into controller
 * methods with {@code @AuthenticationPrincipal AuthenticatedUser user}.
 */
public record AuthenticatedUser(UUID id, String email, Role role) {

    public boolean isStaff() {
        return role.isStaff();
    }

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }
}
