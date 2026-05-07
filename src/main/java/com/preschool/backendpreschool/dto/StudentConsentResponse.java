package com.preschool.backendpreschool.dto;

import com.preschool.backendpreschool.model.StudentConsentType;

import java.time.LocalDateTime;

public record StudentConsentResponse(
        Long studentConsentId,
        Long studentId,
        String studentName,
        Long parentId,
        String parentName,
        Long recordedByUserId,
        String recordedByEmail,
        StudentConsentType consentType,
        Boolean granted,
        Boolean active,
        String notes,
        LocalDateTime acceptedAt,
        LocalDateTime revokedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
