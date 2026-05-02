package com.preschool.backendpreschool.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record PaymentAllocationRequest(
        @NotNull
        Long studentChargeId,

        @NotNull
        @DecimalMin(value = "0.01")
        BigDecimal amountAllocated
) {
}
