package com.preschool.backendpreschool.dto;

import com.preschool.backendpreschool.model.StudentStatus;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

public record StudentRequest(

        @Size(max = 50)
        String studentCode,

        @NotBlank
        @Size(max = 100)
        String firstName,

        @NotBlank
        @Size(max = 100)
        String lastName,

        @NotNull
        LocalDate birthDate,

        Long groupId,

        StudentStatus status,

        @NotNull
        LocalDate enrollmentDate,

        LocalDate withdrawalDate,

        String medicalNotes,

        String allergies,

        String notes
) {
}