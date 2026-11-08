package com.carrental.dashboard;

import com.carrental.dashboard.dto.DashboardSummary;
import com.carrental.dashboard.dto.RevenuePoint;
import com.carrental.dashboard.dto.TopCar;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/dashboard")
@Tag(name = "Admin: dashboard", description = "Business statistics")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/summary")
    @Operation(summary = "Operational overview (ADMIN, AGENT)",
            description = "Fleet status, today's pickups and returns, overdue rentals, revenue this month.")
    public DashboardSummary summary() {
        return dashboardService.summary();
    }

    @GetMapping("/revenue")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Net revenue over time (ADMIN)", description = "Dates are inclusive, max 366 days.")
    @ApiResponse(responseCode = "422", description = "Invalid date range")
    public List<RevenuePoint> revenue(
            @Parameter(example = "2026-01-01") @RequestParam LocalDate from,
            @Parameter(example = "2026-12-31") @RequestParam LocalDate to,
            @RequestParam(defaultValue = "DAY") Granularity granularity) {
        return dashboardService.revenue(from, to, granularity);
    }

    @GetMapping("/top-cars")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Most rented cars (ADMIN)", description = "Based on rentals completed in the date range.")
    @ApiResponse(responseCode = "422", description = "Invalid date range")
    public List<TopCar> topCars(
            @RequestParam LocalDate from,
            @RequestParam LocalDate to,
            @RequestParam(defaultValue = "5") @Min(1) @Max(50) int limit) {
        return dashboardService.topCars(from, to, limit);
    }
}
