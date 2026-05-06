package com.preschool.backendpreschool.dto;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

public record DashboardSummaryResponse(
        LocalDate date,
        YearMonth month,
        DashboardCountsResponse counts,
        DashboardFinanceSummaryResponse finance,
        List<DashboardMaterialAlertResponse> lowStockMaterials,
        List<DashboardScheduleItemResponse> todaySchedule,
        List<DashboardBirthdayResponse> upcomingBirthdays
) {
}
