package com.preschool.backendpreschool.dto;

import java.time.LocalDate;
import java.util.List;

public record DashboardAdminSummaryResponse(
        LocalDate date,
        long totalStudents,
        long activeStudents,
        long totalParents,
        long activeParents,
        long totalMaterials,
        long lowStockMaterials,
        long todayScheduleSlots,
        List<DashboardMaterialAlertResponse> lowStockMaterialAlerts,
        List<DashboardScheduleItemResponse> todaySchedule,
        List<DashboardBirthdayResponse> upcomingBirthdays
) {
}
