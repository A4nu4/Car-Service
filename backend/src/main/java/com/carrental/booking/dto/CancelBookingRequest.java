package com.carrental.booking.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

public record CancelBookingRequest(
        @Size(max = 500) String reason,
        @Schema(description = "Staff only: skip the late-cancellation fee")
        boolean waiveFee
) {
}
