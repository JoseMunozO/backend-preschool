package com.preschool.backendpreschool.dto;

public record StudentHealthReportEntryResponse(
        Long studentId,
        String studentName,
        Long groupId,
        String groupName,
        String allergies,
        String medicalNotes
) {
}
