package com.carrental.location;

import com.carrental.location.dto.LocationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/locations")
@SecurityRequirements
@Tag(name = "Locations", description = "Rental branches (public)")
public class LocationController {

    private final LocationService locationService;

    public LocationController(LocationService locationService) {
        this.locationService = locationService;
    }

    @GetMapping
    @Operation(summary = "List active branches", description = "Not paginated: the number of branches is small.")
    public List<LocationResponse> list() {
        return locationService.listActive();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a branch")
    @ApiResponse(responseCode = "404", description = "Not found or inactive")
    public LocationResponse get(@PathVariable UUID id) {
        return locationService.getActive(id);
    }
}
