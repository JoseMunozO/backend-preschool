package com.preschool.backendpreschool.dto;

import com.preschool.backendpreschool.model.MaterialStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MaterialRequest(
        @Size(max = 50)
        String sku,

        @NotBlank
        @Size(max = 150)
        String name,

        @Size(max = 100)
        String category,

        @Size(max = 50)
        String unit,

        @NotNull
        @Min(0)
        Integer quantityOnHand,

        @NotNull
        @Min(0)
        Integer minimumQuantity,

        MaterialStatus status,

        String notes
) {
}
