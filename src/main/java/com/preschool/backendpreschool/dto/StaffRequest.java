package com.preschool.backendpreschool.dto;

import com.preschool.backendpreschool.model.RoleName;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.Set;

public record StaffRequest(
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

        @Size(max = 50)
        String employeeCode,

        @NotBlank
        @Size(max = 100)
        String positionTitle,

        @NotBlank
        @Size(max = 30)
        String staffType,

        LocalDate hireDate,

        @Future
        LocalDate accessExpiresAt,

        String notes,

        @Size(min = 6, max = 100)
        String password,

        Set<RoleName> roles
) {
}
