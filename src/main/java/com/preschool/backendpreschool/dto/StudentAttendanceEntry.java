package com.preschool.backendpreschool.dto;

import com.preschool.backendpreschool.model.StudentAttendanceStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record StudentAttendanceEntry(
        @NotNull
        Long studentId,

        @NotNull
        StudentAttendanceStatus status,

        @Size(max = 500)
        String notes
) {
}
