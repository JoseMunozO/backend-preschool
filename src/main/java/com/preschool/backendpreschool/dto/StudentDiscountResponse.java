package com.preschool.backendpreschool.dto;

import com.preschool.backendpreschool.model.DiscountType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record StudentDiscountResponse(
        Long studentDiscountId,
        Long studentId,
        String studentName,
        DiscountType discountType,
        BigDecimal value,
        String reason,
        LocalDate validFrom,
        LocalDate validUntil,
        Boolean active,
        Long createdByUserId,
        String createdByEmail,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
