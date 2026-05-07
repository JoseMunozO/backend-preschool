package com.preschool.backendpreschool.dto;

import java.time.LocalDate;

public record DashboardMainSummaryResponse(
        LocalDate date,
        DashboardAdminSummaryResponse administration,
        DashboardFinanceAreaSummaryResponse finance
) {
}
