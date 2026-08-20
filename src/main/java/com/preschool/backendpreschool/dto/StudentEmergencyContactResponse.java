package com.preschool.backendpreschool.dto;

import java.time.LocalDateTime;

public record StudentEmergencyContactResponse(
        Long studentEmergencyContactId,
        Long studentId,
        String studentName,
        String fullName,
        String relationship,
        String phone,
        String alternatePhone,
        String notes,
        Boolean primary,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
