package com.carrental.auth;

import com.carrental.auth.dto.AuthResponse;
import com.carrental.auth.dto.LoginRequest;
import com.carrental.auth.dto.RegisterRequest;
import com.carrental.common.exception.ApiException;
import com.carrental.common.exception.ErrorCode;
import com.carrental.customer.CustomerService;
import com.carrental.security.AccessTokenService;
import com.carrental.user.Role;
import com.carrental.user.User;
import com.carrental.user.UserRepository;
import com.carrental.user.dto.UserResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository users;
    private final CustomerService customerService;
    private final PasswordEncoder passwordEncoder;
    private final AccessTokenService accessTokens;
    private final RefreshTokenService refreshTokens;
    private final Clock clock;
    /** Compared against when the email is unknown, so response time does not reveal which emails exist. */
    private final String dummyPasswordHash;

    public AuthService(UserRepository users, CustomerService customerService, PasswordEncoder passwordEncoder,
                       AccessTokenService accessTokens, RefreshTokenService refreshTokens, Clock clock) {
        this.users = users;
        this.customerService = customerService;
        this.passwordEncoder = passwordEncoder;
        this.accessTokens = accessTokens;
        this.refreshTokens = refreshTokens;
        this.clock = clock;
        this.dummyPasswordHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    /** Self-registration is for customers only; staff accounts are created by an admin. */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = User.normalizeEmail(request.email());
        if (users.existsByEmail(email)) {
            throw emailTaken();
        }
        User user = new User(email, passwordEncoder.encode(request.password()), request.firstName().trim(),
                request.lastName().trim(), trimToNull(request.phone()), Role.CUSTOMER);
        try {
            users.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            // Two registrations with the same email raced past the existsByEmail check.
            throw emailTaken();
        }
        customerService.createEmptyProfile(user);
        user.recordLogin(Instant.now(clock));
        return issueTokens(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = users.findByEmail(User.normalizeEmail(request.email())).orElse(null);
        String hashToCheck = user != null ? user.getPasswordHash() : dummyPasswordHash;
        boolean passwordMatches = passwordEncoder.matches(request.password(), hashToCheck);
        if (user == null || !passwordMatches) {
            throw new ApiException(ErrorCode.INVALID_CREDENTIALS, "Email or password is incorrect");
        }
        if (!user.isEnabled()) {
            throw new ApiException(ErrorCode.ACCOUNT_DISABLED, "This account has been disabled");
        }
        user.recordLogin(Instant.now(clock));
        return issueTokens(user);
    }

    @Transactional
    public AuthResponse refresh(String rawRefreshToken) {
        UUID userId = refreshTokens.consume(rawRefreshToken);
        User user = users.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_REFRESH_TOKEN, "The refresh token is invalid"));
        return issueTokens(user);
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        refreshTokens.revoke(rawRefreshToken);
    }

    private AuthResponse issueTokens(User user) {
        AccessTokenService.IssuedToken access = accessTokens.issue(user);
        RefreshTokenService.IssuedRefreshToken refresh = refreshTokens.issue(user);
        return new AuthResponse("Bearer", access.value(), access.expiresAt(), refresh.value(), refresh.expiresAt(),
                UserResponse.from(user));
    }

    private static ApiException emailTaken() {
        return new ApiException(ErrorCode.EMAIL_ALREADY_REGISTERED, "An account with this email already exists");
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
