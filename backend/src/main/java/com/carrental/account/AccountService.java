package com.carrental.account;

import com.carrental.account.dto.AccountResponse;
import com.carrental.account.dto.ChangePasswordRequest;
import com.carrental.account.dto.UpdateAccountRequest;
import com.carrental.common.exception.ApiException;
import com.carrental.common.exception.ErrorCode;
import com.carrental.customer.CustomerService;
import com.carrental.customer.dto.DriverProfileResponse;
import com.carrental.user.Role;
import com.carrental.user.User;
import com.carrental.user.UserRepository;
import com.carrental.user.UserSessionsInvalidatedEvent;
import com.carrental.user.dto.UserResponse;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** Self-service operations on the caller's own account. */
@Service
@Transactional
public class AccountService {

    private final UserRepository users;
    private final CustomerService customerService;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher events;

    public AccountService(UserRepository users, CustomerService customerService, PasswordEncoder passwordEncoder,
                          ApplicationEventPublisher events) {
        this.users = users;
        this.customerService = customerService;
        this.passwordEncoder = passwordEncoder;
        this.events = events;
    }

    @Transactional(readOnly = true)
    public AccountResponse get(UUID userId) {
        return toResponse(find(userId));
    }

    public AccountResponse update(UUID userId, UpdateAccountRequest request) {
        User user = find(userId);
        String phone = request.phone() == null || request.phone().isBlank() ? null : request.phone().trim();
        user.updateDetails(request.firstName().trim(), request.lastName().trim(), phone);
        return toResponse(user);
    }

    /** Changing the password ends every other session; the caller must log in again. */
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        User user = find(userId);
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new ApiException(ErrorCode.INVALID_CREDENTIALS, "The current password is incorrect");
        }
        user.changePassword(passwordEncoder.encode(request.newPassword()));
        events.publishEvent(new UserSessionsInvalidatedEvent(userId));
    }

    private AccountResponse toResponse(User user) {
        DriverProfileResponse profile = user.getRole() == Role.CUSTOMER
                ? customerService.getDriverProfile(user.getId())
                : null;
        return new AccountResponse(UserResponse.from(user), profile);
    }

    private User find(UUID userId) {
        return users.findById(userId).orElseThrow(() -> ApiException.notFound("User", userId));
    }
}
