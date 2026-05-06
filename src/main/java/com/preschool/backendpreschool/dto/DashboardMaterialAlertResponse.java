package com.preschool.backendpreschool.dto;

public record DashboardMaterialAlertResponse(
        Long materialId,
        String sku,
        String name,
        String category,
        Integer quantityOnHand,
        Integer minimumQuantity,
        Integer shortage
) {
}
