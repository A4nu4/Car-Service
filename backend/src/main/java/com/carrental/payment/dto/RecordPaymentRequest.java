package com.carrental.payment.dto;

import com.carrental.payment.PaymentMethod;
import com.carrental.payment.PaymentType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record RecordPaymentRequest(
        @NotNull @DecimalMin(value = "0.01") @Digits(integer = 10, fraction = 2) BigDecimal amount,
        @NotNull PaymentMethod method,
        @Schema(description = "Defaults to CHARGE") PaymentType type,
        @Size(max = 100) @Schema(description = "Card terminal receipt, transfer id, ...") String reference
) {
}
