package com.preschool.backendpreschool.dto;

import com.preschool.backendpreschool.model.MaterialMovementType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record MaterialMovementRequest(
        @NotNull
        MaterialMovementType movementType,

        @NotNull
        @Min(1)
        Integer quantity,

        String notes
) {
}
