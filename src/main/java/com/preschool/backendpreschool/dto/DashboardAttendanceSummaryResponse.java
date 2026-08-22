package com.preschool.backendpreschool.dto;

public record DashboardAttendanceSummaryResponse(
        long presentCount,
        long absentCount,
        long sickCount,
        long lateCount,
        long unmarkedCount
) {
}
