package com.carrental.user;

import com.carrental.common.exception.ApiException;
import com.carrental.common.exception.ErrorCode;
import com.carrental.security.AuthenticatedUser;
import com.carrental.user.dto.CreateStaffRequest;
import com.carrental.user.dto.UpdateUserRequest;
import com.carrental.user.dto.UserResponse;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** Account administration performed by ADMIN users. */
@Service
@Transactional
public class UserAdminService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher events;

    public UserAdminService(UserRepository users, PasswordEncoder passwordEncoder, ApplicationEventPublisher events) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.events = events;
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> list(Role role, String search, Pageable pageable) {
        Specification<User> spec = Specification.allOf(
                UserSpecifications.hasRoleIn(role == null ? null : List.of(role)),
                UserSpecifications.matchesText(search));
        return users.findAll(spec, pageable).map(UserResponse::from);
    }

    @Transactional(readOnly = true)
    public UserResponse get(UUID id) {
        return UserResponse.from(find(id));
    }

    public UserResponse createStaff(CreateStaffRequest request) {
        if (!request.role().isStaff()) {
            throw ApiException.businessRule("Staff accounts must have role ADMIN or AGENT");
        }
        String email = User.normalizeEmail(request.email());
        if (users.existsByEmail(email)) {
            throw new ApiException(ErrorCode.EMAIL_ALREADY_REGISTERED, "An account with this email already exists");
        }
        User user = new User(email, passwordEncoder.encode(request.password()), request.firstName().trim(),
                request.lastName().trim(), blankToNull(request.phone()), request.role());
        return UserResponse.from(users.save(user));
    }

    public UserResponse update(UUID id, UpdateUserRequest request, AuthenticatedUser actor) {
        User user = find(id);
        user.updateDetails(request.firstName().trim(), request.lastName().trim(), blankToNull(request.phone()));
        if (request.role() != null && request.role() != user.getRole()) {
            if (!user.getRole().isStaff() || !request.role().isStaff()) {
                throw ApiException.businessRule("Only staff accounts can change role, and only between ADMIN and AGENT");
            }
            if (user.getId().equals(actor.id())) {
                throw ApiException.businessRule("You cannot change your own role");
            }
            user.changeRole(request.role());
            events.publishEvent(new UserSessionsInvalidatedEvent(user.getId()));
        }
        return UserResponse.from(user);
    }

    public UserResponse setEnabled(UUID id, boolean enabled, AuthenticatedUser actor) {
        User user = find(id);
        if (!enabled && user.getId().equals(actor.id())) {
            throw ApiException.businessRule("You cannot disable your own account");
        }
        user.setEnabled(enabled);
        if (!enabled) {
            events.publishEvent(new UserSessionsInvalidatedEvent(user.getId()));
        }
        return UserResponse.from(user);
    }

    private User find(UUID id) {
        return users.findById(id).orElseThrow(() -> ApiException.notFound("User", id));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
