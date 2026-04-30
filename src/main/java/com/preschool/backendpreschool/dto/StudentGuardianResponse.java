package com.preschool.backendpreschool.dto;

import com.preschool.backendpreschool.model.GuardianRelationshipType;

import java.time.LocalDateTime;

public record StudentGuardianResponse(
        Long studentId,
        String studentName,
        Long parentId,
        String parentName,
        GuardianRelationshipType relationshipType,
        Boolean primaryContact,
        Boolean billingContact,
        Boolean authorizedPickup,
        Boolean livesWithStudent,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
