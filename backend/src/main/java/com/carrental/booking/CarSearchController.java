package com.carrental.booking;

import com.carrental.booking.dto.CarSearchParams;
import com.carrental.booking.dto.PriceQuoteResponse;
import com.carrental.car.dto.CarResponse;
import com.carrental.common.web.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cars")
@SecurityRequirements
@Tag(name = "Cars", description = "Search the rentable fleet (public)")
public class CarSearchController {

    private final CarSearchService carSearchService;

    public CarSearchController(CarSearchService carSearchService) {
        this.carSearchService = carSearchService;
    }

    @GetMapping
    @Operation(summary = "Search cars",
            description = "Without pickupAt/dropoffAt this browses the catalogue. With both, only cars that "
                    + "are free for the whole period are returned. Sort e.g. by pricePerDay,asc.")
    @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR (e.g. only one of pickupAt/dropoffAt)")
    public PageResponse<CarResponse> search(
            @Valid @ParameterObject CarSearchParams params,
            @ParameterObject @PageableDefault(sort = "pricePerDay") Pageable pageable) {
        return PageResponse.of(carSearchService.search(params, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a car")
    @ApiResponse(responseCode = "404", description = "Not found or not currently rentable")
    public CarResponse get(@PathVariable UUID id) {
        return carSearchService.get(id);
    }

    @GetMapping("/{id}/quote")
    @Operation(summary = "Price and availability for a period",
            description = "Use this to show the total before booking. It does not reserve the car.")
    @ApiResponse(responseCode = "404", description = "Not found or not currently rentable")
    @ApiResponse(responseCode = "422", description = "dropoffAt is not after pickupAt")
    public PriceQuoteResponse quote(
            @PathVariable UUID id,
            @Parameter(description = "ISO-8601 instant, e.g. 2026-12-01T09:00:00Z") @RequestParam Instant pickupAt,
            @Parameter(description = "ISO-8601 instant, e.g. 2026-12-04T09:00:00Z") @RequestParam Instant dropoffAt) {
        return carSearchService.quote(id, pickupAt, dropoffAt);
    }
}
