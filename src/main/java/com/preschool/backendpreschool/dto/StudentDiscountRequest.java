package com.preschool.backendpreschool.dto;

import com.preschool.backendpreschool.model.DiscountType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record StudentDiscountRequest(
        @NotNull
        DiscountType discountType,

        @NotNull
        @DecimalMin(value = "0.00")
        BigDecimal value,

        @NotBlank
        @Size(max = 255)
        String reason,

        @NotNull
        LocalDate validFrom,

        LocalDate validUntil
) {
}
