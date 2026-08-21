package com.preschool.backendpreschool.dto;

import com.preschool.backendpreschool.model.MaterialStatus;

import java.time.LocalDateTime;

public record MaterialResponse(
        Long materialId,
        String sku,
        String name,
        String category,
        String unit,
        Integer quantityOnHand,
        Integer minimumQuantity,
        Boolean lowStock,
        MaterialStatus status,
        String notes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime deletedAt
) {
}
