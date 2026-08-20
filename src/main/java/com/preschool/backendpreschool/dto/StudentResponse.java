package com.preschool.backendpreschool.dto;


import com.preschool.backendpreschool.model.StudentStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record StudentResponse(
        Long studentId,
        String studentCode,
        String firstName,
        String lastName,
        String profilePhotoUrl,
        LocalDate birthDate,
        Long groupId,
        String groupName,
        String primaryGuardianName,
        StudentStatus status,
        LocalDate enrollmentDate,
        LocalDate withdrawalDate,
        String medicalNotes,
        String allergies,
        String notes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
