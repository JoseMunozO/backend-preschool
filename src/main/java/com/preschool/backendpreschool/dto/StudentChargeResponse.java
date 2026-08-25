package com.preschool.backendpreschool.dto;

import com.preschool.backendpreschool.model.DiscountType;
import com.preschool.backendpreschool.model.StudentChargeStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record StudentChargeResponse(
        Long studentChargeId,
        Long studentId,
        String studentName,
        Long chargeTypeId,
        String chargeTypeCode,
        String chargeTypeName,
        LocalDate dueDate,
        LocalDate billingPeriodStart,
        LocalDate billingPeriodEnd,
        BigDecimal amountDue,
        BigDecimal amountPaid,
        BigDecimal balance,
        StudentChargeStatus status,
        String description,
        BigDecimal originalAmount,
        DiscountType discountType,
        BigDecimal discountValue,
        String discountReason,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
