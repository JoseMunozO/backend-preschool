package com.preschool.backendpreschool.dto;

import com.preschool.backendpreschool.model.ParentStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ParentRequest(
        @NotBlank
        @Size(max = 100)
        String firstName,

        @NotBlank
        @Size(max = 100)
        String lastName,

        @Email
        @Size(max = 150)
        String email,

        @Size(max = 30)
        String phone,

        @Size(max = 255)
        String address,

        @Size(max = 20)
        String preferredLanguage,

        ParentStatus status,

        String notes,

        @Size(min = 6, max = 100)
        String password
) {
}
