package com.preschool.backendpreschool.dto;

import com.preschool.backendpreschool.model.DiscountType;
import com.preschool.backendpreschool.model.StudentChargeStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * discountType/discountValue/discountReason are optional and only meaningful together: to apply
 * a discount at creation time, all three must be provided (amountDue is treated as the
 * pre-discount amount in that case). Omit all three for a plain, undiscounted charge.
 */
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
        String description,

        DiscountType discountType,

        @DecimalMin(value = "0.00")
        BigDecimal discountValue,

        @Size(max = 255)
        String discountReason
) {
}
