package com.preschool.backendpreschool.dto;

import com.preschool.backendpreschool.model.DiscountDurationType;
import com.preschool.backendpreschool.model.DiscountType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * validFrom/validUntil are only meaningful for {@link DiscountDurationType#SCHEDULED} - an
 * {@link DiscountDurationType#INSTANT} discount always starts today and is computed to cover only
 * the current billing cycle, regardless of what (if anything) is sent here.
 */
public record StudentDiscountRequest(
        @NotNull
        DiscountType discountType,

        @NotNull
        DiscountDurationType durationType,

        @NotNull
        @DecimalMin(value = "0.00")
        BigDecimal value,

        @NotBlank
        @Size(max = 255)
        String reason,

        LocalDate validFrom,

        LocalDate validUntil
) {
}
