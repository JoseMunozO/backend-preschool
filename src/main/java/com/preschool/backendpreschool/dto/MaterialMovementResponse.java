package com.preschool.backendpreschool.dto;

import com.preschool.backendpreschool.model.MaterialMovementType;

import java.time.LocalDateTime;

public record MaterialMovementResponse(
        Long materialMovementId,
        Long materialId,
        String materialName,
        MaterialMovementType movementType,
        Integer quantity,
        LocalDateTime movementDate,
        Long performedByUserId,
        String performedByEmail,
        String notes,
        LocalDateTime createdAt
) {
}
