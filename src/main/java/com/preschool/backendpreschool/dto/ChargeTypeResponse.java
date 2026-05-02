package com.preschool.backendpreschool.dto;

import com.preschool.backendpreschool.model.ChargeRecurrenceType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ChargeTypeResponse(
        Long chargeTypeId,
        String code,
        String name,
        ChargeRecurrenceType recurrenceType,
        BigDecimal defaultAmount,
        Boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
