package com.preschool.backendpreschool.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentAllocationResponse(
        Long paymentAllocationId,
        Long studentChargeId,
        Long studentId,
        String studentName,
        BigDecimal amountAllocated,
        LocalDateTime createdAt
) {
}
