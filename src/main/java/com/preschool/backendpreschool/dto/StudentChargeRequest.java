package com.preschool.backendpreschool.dto;

import com.preschool.backendpreschool.model.StudentChargeStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record StudentChargeRequest(
        @NotNull
        Long studentId,

        @NotNull
        Long chargeTypeId,

        @NotNull
        LocalDate dueDate,

        LocalDate billingPeriodStart,
        LocalDate billingPeriodEnd,

        @NotNull
        @DecimalMin(value = "0.00")
        BigDecimal amountDue,

        StudentChargeStatus status,

        @Size(max = 255)
        String description
) {
}
