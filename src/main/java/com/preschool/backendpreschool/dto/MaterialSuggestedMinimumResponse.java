package com.preschool.backendpreschool.dto;

import com.preschool.backendpreschool.model.MaterialConsumptionWindow;

public record MaterialSuggestedMinimumResponse(
        Long materialId,
        Integer currentMinimumQuantity,
        MaterialConsumptionWindow window,
        double averageMonthlyConsumption,
        Integer suggestedMinimumQuantity,
        boolean hasData
) {
}
