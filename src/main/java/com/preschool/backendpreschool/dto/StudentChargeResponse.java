package com.preschool.backendpreschool.dto;

import com.preschool.backendpreschool.model.DiscountType;
import com.preschool.backendpreschool.model.StudentChargeStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

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
        /** Computed live from how many months past dueDate this charge is - never stored, so it's always current. Zero unless status is OVERDUE. */
        BigDecimal lateFeeAmount,
        /** Payments that allocated money to this charge - each has a receipt PDF at GET /api/payments/{paymentId}/receipt. Empty until any payment is made. */
        List<Long> paymentIds,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
