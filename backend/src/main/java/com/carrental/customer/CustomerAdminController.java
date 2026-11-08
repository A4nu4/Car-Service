package com.carrental.customer;

import com.carrental.common.web.PageResponse;
import com.carrental.customer.dto.CustomerDetailResponse;
import com.carrental.user.dto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** Staff (ADMIN, AGENT) look up customers, e.g. to book on their behalf at the desk. */
@RestController
@RequestMapping("/api/v1/admin/customers")
@Tag(name = "Admin: customers", description = "Customer lookup for staff (ADMIN, AGENT)")
public class CustomerAdminController {

    private final CustomerService customerService;

    public CustomerAdminController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping
    @Operation(summary = "Search customers")
    public PageResponse<UserResponse> list(
            @Parameter(description = "Case-insensitive search in email, first and last name")
            @RequestParam(required = false) String search,
            @ParameterObject @PageableDefault(sort = "lastName", direction = Sort.Direction.ASC) Pageable pageable) {
        return PageResponse.of(customerService.listCustomers(search, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a customer with their driver profile",
            description = "Use GET /api/v1/bookings?customerId={id} for the customer's rental history.")
    @ApiResponse(responseCode = "404", description = "No customer with this id")
    public CustomerDetailResponse get(@PathVariable UUID id) {
        return customerService.getCustomer(id);
    }
}
