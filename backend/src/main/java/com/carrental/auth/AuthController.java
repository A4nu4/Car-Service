package com.carrental.auth;

import com.carrental.auth.dto.AuthResponse;
import com.carrental.auth.dto.LoginRequest;
import com.carrental.auth.dto.RefreshTokenRequest;
import com.carrental.auth.dto.RegisterRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@SecurityRequirements
@Tag(name = "Authentication", description = "Registration, login and token refresh (public)")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register a customer account", description = "Returns tokens, so the user is logged in immediately.")
    @ApiResponse(responseCode = "201", description = "Registered and logged in")
    @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR")
    @ApiResponse(responseCode = "409", description = "EMAIL_ALREADY_REGISTERED")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    @Operation(summary = "Log in with email and password")
    @ApiResponse(responseCode = "200", description = "Logged in")
    @ApiResponse(responseCode = "401", description = "INVALID_CREDENTIALS")
    @ApiResponse(responseCode = "403", description = "ACCOUNT_DISABLED")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Exchange a refresh token for a new token pair",
            description = "The submitted refresh token becomes invalid. Do not send two refresh requests in "
                    + "parallel with the same token: the second one counts as reuse and ends all sessions.")
    @ApiResponse(responseCode = "200", description = "New token pair")
    @ApiResponse(responseCode = "401", description = "INVALID_REFRESH_TOKEN")
    public AuthResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return authService.refresh(request.refreshToken());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Revoke a refresh token", description = "Always returns 204, even for unknown tokens.")
    @ApiResponse(responseCode = "204", description = "Logged out")
    public void logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request.refreshToken());
    }
}
