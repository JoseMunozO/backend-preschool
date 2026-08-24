package com.preschool.backendpreschool.dto;

public record AttendanceReportEntryResponse(
        Long studentId,
        String studentName,
        Long groupId,
        String groupName,
        long presentCount,
        long absentCount,
        long lateCount,
        long sickCount,
        long unmarkedCount
) {
}
