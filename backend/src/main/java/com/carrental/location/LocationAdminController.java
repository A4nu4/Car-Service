package com.carrental.location;

import com.carrental.location.dto.LocationRequest;
import com.carrental.location.dto.LocationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/locations")
@Tag(name = "Admin: locations", description = "Manage branches (read: ADMIN, AGENT; write: ADMIN)")
public class LocationAdminController {

    private final LocationService locationService;

    public LocationAdminController(LocationService locationService) {
        this.locationService = locationService;
    }

    @GetMapping
    @Operation(summary = "List all branches, including inactive ones")
    public List<LocationResponse> list() {
        return locationService.listAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a branch")
    @ApiResponse(responseCode = "201", description = "Created")
    @ApiResponse(responseCode = "409", description = "DUPLICATE_RESOURCE (name taken)")
    public LocationResponse create(@Valid @RequestBody LocationRequest request) {
        return locationService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update a branch")
    @ApiResponse(responseCode = "404", description = "Not found")
    @ApiResponse(responseCode = "409", description = "DUPLICATE_RESOURCE (name taken)")
    public LocationResponse update(@PathVariable UUID id, @Valid @RequestBody LocationRequest request) {
        return locationService.update(id, request);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Activate or deactivate a branch")
    public LocationResponse setStatus(@PathVariable UUID id, @Valid @RequestBody LocationStatusRequest request) {
        return locationService.setActive(id, request.active());
    }

    public record LocationStatusRequest(@NotNull Boolean active) {
    }
}
