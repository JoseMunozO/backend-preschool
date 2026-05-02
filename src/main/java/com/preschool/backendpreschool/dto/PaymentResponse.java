package com.preschool.backendpreschool.dto;

import com.preschool.backendpreschool.model.PaymentMethod;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record PaymentResponse(
        Long paymentId,
        Long parentId,
        String parentName,
        Long receivedByStaffId,
        String receivedByStaffName,
        LocalDate paymentDate,
        BigDecimal totalAmount,
        PaymentMethod paymentMethod,
        String referenceNumber,
        String notes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<PaymentAllocationResponse> allocations
) {
}
