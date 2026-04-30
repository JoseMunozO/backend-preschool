package com.preschool.backendpreschool.dto;

import com.preschool.backendpreschool.model.GuardianRelationshipType;
import jakarta.validation.constraints.NotNull;

public record StudentGuardianRequest(
        @NotNull
        Long studentId,

        @NotNull
        GuardianRelationshipType relationshipType,

        Boolean primaryContact,
        Boolean billingContact,
        Boolean authorizedPickup,
        Boolean livesWithStudent
) {
}
