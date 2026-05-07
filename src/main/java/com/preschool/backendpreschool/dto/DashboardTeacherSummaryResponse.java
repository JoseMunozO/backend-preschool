package com.preschool.backendpreschool.dto;

import java.time.LocalDate;
import java.util.List;

public record DashboardTeacherSummaryResponse(
        LocalDate date,
        long activeStudents,
        long todayScheduleSlots,
        long lowStockMaterials,
        List<DashboardScheduleItemResponse> todaySchedule,
        List<DashboardBirthdayResponse> upcomingBirthdays,
        List<DashboardMaterialAlertResponse> lowStockMaterialAlerts
) {
}
