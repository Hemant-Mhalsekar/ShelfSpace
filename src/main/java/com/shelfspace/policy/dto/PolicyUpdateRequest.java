package com.shelfspace.policy.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** All fields are required: a PUT replaces the whole rule set for a tier. */
public record PolicyUpdateRequest(
        @NotNull @Min(1) @Max(50) Integer maxBooks,
        @NotNull @Min(1) @Max(365) Integer loanDays,
        @NotNull @Min(0) @Max(10) Integer maxRenewals,
        @NotNull @DecimalMin("0.00") @DecimalMax("1000.00") @Digits(integer = 8, fraction = 2) BigDecimal finePerDay,
        @NotNull @DecimalMin("0.00") @Digits(integer = 8, fraction = 2) BigDecimal fineCap,
        @NotNull @DecimalMin("0.00") @Digits(integer = 8, fraction = 2) BigDecimal maxUnpaidFines,
        @NotNull @Min(0) @Max(20) Integer maxReservations,
        @NotNull @Min(1) @Max(336) Integer holdHours
) {
}
