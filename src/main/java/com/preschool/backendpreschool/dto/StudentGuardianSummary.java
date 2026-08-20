package com.preschool.backendpreschool.dto;

import com.preschool.backendpreschool.model.GuardianRelationshipType;

public record StudentGuardianSummary(
        Long parentId,
        String parentName,
        String email,
        String phone,
        GuardianRelationshipType relationshipType,
        Boolean primaryContact,
        Boolean billingContact,
        Boolean authorizedPickup,
        Boolean livesWithStudent
) {
}
