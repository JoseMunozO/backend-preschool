package com.preschool.backendpreschool.dto;

import com.preschool.backendpreschool.model.StudentNoteType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record StudentNoteRequest(
        @NotNull
        StudentNoteType noteType,

        @NotBlank
        @Size(max = 5000)
        String content
) {
}
