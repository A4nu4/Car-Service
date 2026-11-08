package com.carrental.account;

import com.carrental.account.dto.AccountResponse;
import com.carrental.account.dto.ChangePasswordRequest;
import com.carrental.account.dto.UpdateAccountRequest;
import com.carrental.customer.CustomerService;
import com.carrental.customer.dto.DriverProfileRequest;
import com.carrental.customer.dto.DriverProfileResponse;
import com.carrental.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/account")
@Tag(name = "Account", description = "The logged-in user's own account (any role)")
public class AccountController {

    private final AccountService accountService;
    private final CustomerService customerService;

    public AccountController(AccountService accountService, CustomerService customerService) {
        this.accountService = accountService;
        this.customerService = customerService;
    }

    @GetMapping
    @Operation(summary = "Get the current user", description = "Customers also get their driver profile.")
    @ApiResponse(responseCode = "401", description = "UNAUTHENTICATED")
    public AccountResponse me(@AuthenticationPrincipal AuthenticatedUser user) {
        return accountService.get(user.id());
    }

    @PutMapping
    @Operation(summary = "Update name and phone")
    public AccountResponse update(@AuthenticationPrincipal AuthenticatedUser user,
                                  @Valid @RequestBody UpdateAccountRequest request) {
        return accountService.update(user.id(), request);
    }

    @PutMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Change password", description = "Revokes all refresh tokens; log in again afterwards.")
    @ApiResponse(responseCode = "204", description = "Password changed")
    @ApiResponse(responseCode = "401", description = "INVALID_CREDENTIALS (wrong current password)")
    public void changePassword(@AuthenticationPrincipal AuthenticatedUser user,
                               @Valid @RequestBody ChangePasswordRequest request) {
        accountService.changePassword(user.id(), request);
    }

    @PutMapping("/driver-profile")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Set driver details (customers)",
            description = "Required before booking: date of birth and a driving licence valid through the rental.")
    @ApiResponse(responseCode = "403", description = "Caller is not a customer")
    public DriverProfileResponse updateDriverProfile(@AuthenticationPrincipal AuthenticatedUser user,
                                                     @Valid @RequestBody DriverProfileRequest request) {
        return customerService.updateDriverProfile(user.id(), request);
    }
}
