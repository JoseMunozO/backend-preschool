package com.preschool.backendpreschool.dto;

import com.preschool.backendpreschool.model.StudentConsentType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record StudentConsentRequest(
        Long parentId,

        @NotNull
        StudentConsentType consentType,

        @Size(max = 5000)
        String notes
) {
}
