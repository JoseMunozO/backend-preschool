package com.preschool.backendpreschool.controller;

import com.preschool.backendpreschool.dto.DashboardAdminSummaryResponse;
import com.preschool.backendpreschool.dto.DashboardFinanceAreaSummaryResponse;
import com.preschool.backendpreschool.dto.DashboardTeacherSummaryResponse;
import com.preschool.backendpreschool.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/teacher-summary")
    public DashboardTeacherSummaryResponse getTeacherSummary() {
        return dashboardService.getTeacherSummary();
    }

    @GetMapping("/admin-summary")
    public DashboardAdminSummaryResponse getAdminSummary() {
        return dashboardService.getAdminSummary();
    }

    @GetMapping("/finance-summary")
    public DashboardFinanceAreaSummaryResponse getFinanceSummary() {
        return dashboardService.getFinanceSummary();
    }
}
