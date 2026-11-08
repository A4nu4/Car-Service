package com.carrental.user;

import com.carrental.common.web.PageResponse;
import com.carrental.security.AuthenticatedUser;
import com.carrental.user.dto.CreateStaffRequest;
import com.carrental.user.dto.UpdateUserRequest;
import com.carrental.user.dto.UpdateUserStatusRequest;
import com.carrental.user.dto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/users")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin: users", description = "Manage staff and customer accounts (ADMIN only)")
public class UserAdminController {

    private final UserAdminService userAdminService;

    public UserAdminController(UserAdminService userAdminService) {
        this.userAdminService = userAdminService;
    }

    @GetMapping
    @Operation(summary = "List users", description = "Filter by role and/or free text (email, first or last name).")
    public PageResponse<UserResponse> list(
            @Parameter(description = "Only users with this role") @RequestParam(required = false) Role role,
            @Parameter(description = "Case-insensitive text search") @RequestParam(required = false) String search,
            @ParameterObject @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return PageResponse.of(userAdminService.list(role, search, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a user")
    @ApiResponse(responseCode = "404", description = "User not found")
    public UserResponse get(@PathVariable UUID id) {
        return userAdminService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a staff account (ADMIN or AGENT)")
    @ApiResponse(responseCode = "201", description = "Created")
    @ApiResponse(responseCode = "409", description = "EMAIL_ALREADY_REGISTERED")
    @ApiResponse(responseCode = "422", description = "Role is not a staff role")
    public UserResponse createStaff(@Valid @RequestBody CreateStaffRequest request) {
        return userAdminService.createStaff(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update name, phone and (for staff) role")
    @ApiResponse(responseCode = "422", description = "Illegal role change")
    public UserResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateUserRequest request,
                               @AuthenticationPrincipal AuthenticatedUser actor) {
        return userAdminService.update(id, request, actor);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Enable or disable an account",
            description = "Disabling immediately revokes the user's refresh tokens. "
                    + "Existing access tokens expire within 15 minutes.")
    @ApiResponse(responseCode = "422", description = "Cannot disable your own account")
    public UserResponse setStatus(@PathVariable UUID id, @Valid @RequestBody UpdateUserStatusRequest request,
                                  @AuthenticationPrincipal AuthenticatedUser actor) {
        return userAdminService.setEnabled(id, request.enabled(), actor);
    }
}
