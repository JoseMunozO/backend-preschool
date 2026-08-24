package com.preschool.backendpreschool.dto;

import com.preschool.backendpreschool.model.ChargeRecurrenceType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ChargeTypeRequest(
        @NotBlank
        @Size(max = 50)
        String code,

        @NotBlank
        @Size(max = 100)
        String name,

        @NotNull
        ChargeRecurrenceType recurrenceType,

        @DecimalMin(value = "0.00")
        BigDecimal defaultAmount,

        Boolean active
) {
}
