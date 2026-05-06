package com.preschool.backendpreschool.dto;

public record DashboardCountsResponse(
        long totalStudents,
        long activeStudents,
        long totalParents,
        long activeParents,
        long totalMaterials,
        long lowStockMaterials,
        long pendingCharges,
        long overdueCharges,
        long todayScheduleSlots
) {
}
