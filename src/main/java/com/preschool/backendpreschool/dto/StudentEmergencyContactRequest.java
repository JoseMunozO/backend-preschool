package com.preschool.backendpreschool.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record StudentEmergencyContactRequest(
        @NotBlank
        @Size(max = 150)
        String fullName,

        @NotBlank
        @Size(max = 100)
        String relationship,

        @NotBlank
        @Size(max = 30)
        String phone,

        @Size(max = 30)
        String alternatePhone,

        @Size(max = 5000)
        String notes,

        Boolean primary
) {
}
