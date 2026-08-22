package com.preschool.backendpreschool.dto;

import com.preschool.backendpreschool.model.StudentAttendanceStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record StudentAttendanceResponse(
        Long studentAttendanceId,
        Long studentId,
        String studentName,
        LocalDate date,
        StudentAttendanceStatus status,
        String notes,
        Long recordedByUserId,
        String recordedByEmail,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
