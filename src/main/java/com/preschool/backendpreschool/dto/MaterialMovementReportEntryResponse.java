package com.preschool.backendpreschool.dto;

import com.preschool.backendpreschool.model.MaterialMovementType;

import java.time.LocalDateTime;

public record MaterialMovementReportEntryResponse(
        Long materialMovementId,
        Long materialId,
        String materialName,
        MaterialMovementType movementType,
        Integer quantity,
        LocalDateTime movementDate,
        Long performedByUserId,
        String performedByEmail,
        String performedByName,
        String notes,
        LocalDateTime createdAt,
        Integer runningBalance
) {
}
