package com.carrental.car;

import com.carrental.car.dto.CarRequest;
import com.carrental.car.dto.FleetCarResponse;
import com.carrental.common.web.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
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
@RequestMapping("/api/v1/admin/cars")
@Tag(name = "Admin: fleet", description = "Manage cars (read: ADMIN, AGENT; write: ADMIN)")
public class FleetAdminController {

    private final FleetService fleetService;

    public FleetAdminController(FleetService fleetService) {
        this.fleetService = fleetService;
    }

    @GetMapping
    @Operation(summary = "List the fleet", description = "All cars regardless of status, with internal fields.")
    public PageResponse<FleetCarResponse> list(
            @RequestParam(required = false) CarStatus status,
            @RequestParam(required = false) UUID locationId,
            @RequestParam(required = false) CarCategory category,
            @Parameter(description = "Search in brand, model and registration number")
            @RequestParam(required = false) String search,
            @ParameterObject @PageableDefault(sort = {"brand", "model"}) Pageable pageable) {
        return PageResponse.of(fleetService.list(status, locationId, category, search, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a car with internal fields")
    @ApiResponse(responseCode = "404", description = "Not found")
    public FleetCarResponse get(@PathVariable UUID id) {
        return fleetService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Add a car to the fleet")
    @ApiResponse(responseCode = "201", description = "Created")
    @ApiResponse(responseCode = "404", description = "Location not found")
    @ApiResponse(responseCode = "409", description = "DUPLICATE_RESOURCE (VIN or registration number taken)")
    public FleetCarResponse create(@Valid @RequestBody CarRequest request) {
        return fleetService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update a car",
            description = "Price changes apply to new bookings only; existing bookings keep their agreed rate.")
    @ApiResponse(responseCode = "409", description = "DUPLICATE_RESOURCE, or INVALID_STATE_TRANSITION when "
            + "moving a car that has open bookings")
    public FleetCarResponse update(@PathVariable UUID id, @Valid @RequestBody CarRequest request) {
        return fleetService.update(id, request);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Change operational status (AVAILABLE, MAINTENANCE, RETIRED)")
    @ApiResponse(responseCode = "409", description = "INVALID_STATE_TRANSITION: car has open bookings")
    public FleetCarResponse changeStatus(@PathVariable UUID id, @Valid @RequestBody CarStatusRequest request) {
        return fleetService.changeStatus(id, request.status());
    }

    public record CarStatusRequest(@NotNull CarStatus status) {
    }
}
