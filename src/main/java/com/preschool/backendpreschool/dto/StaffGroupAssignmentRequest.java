package com.preschool.backendpreschool.dto;

import com.preschool.backendpreschool.model.StaffGroupRole;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record StaffGroupAssignmentRequest(
        @NotNull
        Long staffId,

        @NotNull
        Long groupId,

        StaffGroupRole roleInGroup,

        Boolean primary,

        @NotNull
        LocalDate startDate,

        LocalDate endDate
) {
}
